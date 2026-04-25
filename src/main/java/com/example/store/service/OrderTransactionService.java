package com.example.store.service;

import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.service.analytics.MaterializedViewUpdaterService;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling order creation with inventory management using MongoDB transactions.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: ACID Transactions for Order Creation + Inventory Updates + CQRS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This service ensures that:
 * 1. Customer exists
 * 2. All products exist and have sufficient inventory
 * 3. Order is created successfully
 * 4. Inventory is decremented for all products atomically
 * 5. Materialized views are updated (CQRS)
 * 6. If ANY step fails, EVERYTHING is rolled back
 *
 * Transaction Flow:
 * ┌─────────────────────────────────────────┐
 * │ START TRANSACTION                       │
 * ├─────────────────────────────────────────┤
 * │ 1. Validate customer exists             │
 * │ 2. Validate all products exist          │
 * │ 3. Check inventory availability         │
 * │ 4. Create order document                │
 * │ 5. Atomically decrement inventory ($inc)│
 * ├─────────────────────────────────────────┤
 * │ COMMIT (if all succeed)                 │
 * │ ROLLBACK (if any fail)                  │
 * └─────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * ⚠️  WHY ATOMIC $inc INSTEAD OF READ-MODIFY-WRITE?
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The WRONG approach (race condition):
 *   product.setInventory(product.getInventory() - quantity);  // read
 *   productRepository.save(product);                          // write
 *
 * Problem: Two concurrent orders can BOTH read inventory=5, BOTH decrement
 * to 4, and BOTH save. Result: 2 orders placed but only 1 unit decremented!
 *
 * The RIGHT approach (atomic update):
 *   mongoTemplate.updateFirst(
 *     Query.query(Criteria.where("_id").is(id).and("inventory").gte(qty)),
 *     new Update().inc("inventory", -qty),
 *     Product.class
 *   );
 *
 * The conditional filter (.and("inventory").gte(qty)) + the $inc update
 * happen as a single atomic operation on the MongoDB server. If inventory
 * dropped between our check (step 2) and our update (step 5), the filter
 * won't match, the update returns matchedCount=0, and we throw an exception.
 *
 * IMPORTANT: Requires MongoDB to be running as a replica set!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final MongoTemplate mongoTemplate;
    private final MaterializedViewUpdaterService materializedViewUpdater;

    /**
     * Create a normal order (< 100 items) with inventory validation and updates in a transaction.
     *
     * P1 FIX: Removed separate validateInventory() call to eliminate race condition.
     * Now relies entirely on atomic decrementInventory() which checks and updates in one operation.
     *
     * CQRS: Updates materialized views after order creation.
     *
     * @param order The order to create (must have items populated)
     * @return The saved order
     * @throws CustomerNotFoundException if customer is not found
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientInventoryException if any product has insufficient inventory
     */
    @Transactional
    public Order createOrderWithInventoryUpdate(Order order) {
        resolveCustomerName(order);

        // P1 FIX: Validate products exist first (but don't check inventory)
        // The atomic decrementInventory() will handle inventory validation
        validateProductsExist(order.getItems());

        // Save order first (will be rolled back if inventory fails)
        Order savedOrder = orderRepository.save(order);

        // P1 FIX: Atomic decrement with built-in inventory check
        // This is the ONLY place where inventory is checked - eliminates race condition
        decrementInventory(order.getItems());

        // CQRS: Update materialized views
        try {
            materializedViewUpdater.onOrderCreated(savedOrder);
            log.debug("Materialized views updated for order: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Failed to update materialized views for order: {}", savedOrder.getId(), e);
            // Don't fail the transaction - views can be rebuilt
        }

        return savedOrder;
    }

    /**
     * Create a large order (100+ items) with inventory validation in a transaction.
     *
     * P1 FIX: Removed separate validateInventory() call to eliminate race condition.
     *
     * The items are validated and inventory is decremented, but items are NOT
     * embedded in the saved order document (the caller will create bucket documents
     * instead). This is the Outlier Pattern in action.
     *
     * @param order The order to create (items field will be null in the saved document)
     * @param items The full item list — used for inventory validation and decrement
     * @return The saved order (without embedded items)
     * @throws CustomerNotFoundException if customer is not found
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientInventoryException if any product has insufficient inventory
     */
    @Transactional
    public Order createLargeOrderWithInventoryUpdate(Order order, List<OrderItem> items) {
        resolveCustomerName(order);

        // P1 FIX: Only validate products exist, not inventory
        validateProductsExist(items);

        // Save the order header (items are stored in buckets, not embedded)
        Order savedOrder = orderRepository.save(order);

        // P1 FIX: Atomic decrement handles inventory validation
        decrementInventory(items);

        return savedOrder;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void resolveCustomerName(Order order) {
        String customerId = order.getCustomerId();
        if (customerId != null && !customerId.trim().isEmpty()) {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
            if (order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()) {
                order.setCustomerName(customer.getName());
            }
        }
    }

    /**
     * Validate that all products exist (P1 FIX).
     *
     * This does NOT check inventory - that's done atomically in decrementInventory().
     *
     * Why separate from inventory check:
     * - Product existence is a permanent failure (won't change)
     * - Inventory is transient (can change between checks)
     * - Failing fast on missing products saves a transaction
     */
    private void validateProductsExist(List<OrderItem> items) {
        for (OrderItem item : items) {
            // Just verify product exists - don't check inventory yet
            productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
        }
    }

    /**
     * Atomically decrement inventory for each item using MongoDB's $inc operator (P1 FIX).
     *
     * THIS IS THE ONLY PLACE WHERE INVENTORY IS CHECKED!
     *
     * P1 FIX: No separate validation before this method - eliminates race condition:
     *
     * OLD APPROACH (RACE CONDITION):
     * 1. Check inventory >= quantity (SELECT)
     * 2. [Another thread could order here!]
     * 3. Decrement inventory (UPDATE)
     * 4. Result: Both threads pass step 1, inventory goes negative
     *
     * NEW APPROACH (ATOMIC):
     * 1. Atomic: Check inventory >= quantity AND decrement (single operation)
     * 2. Result: Only one thread succeeds, the other gets matchedCount=0
     *
     * The query filter includes .and("inventory").gte(quantity) so the update
     * only succeeds if inventory is CURRENTLY sufficient at the moment of the update.
     *
     * If the atomic update finds no match (insufficient inventory), an
     * InsufficientInventoryException is thrown and the surrounding @Transactional
     * rolls back all prior changes (order creation, etc.).
     */
    private void decrementInventory(List<OrderItem> items) {
        Map<String, InventoryInfo> insufficient = new HashMap<>();

        for (OrderItem item : items) {
            String productId = item.getProductId();
            int quantity = item.getQuantity();

            // ═══════════════════════════════════════════════════════════════
            // P1 FIX: ATOMIC CONDITIONAL UPDATE - THE ONLY INVENTORY CHECK
            // ═══════════════════════════════════════════════════════════════
            // "Find the product where _id matches AND inventory >= quantity,
            //  then decrement inventory by quantity — all in one server op."
            // ═══════════════════════════════════════════════════════════════
            Query query = Query.query(
                Criteria.where("_id").is(productId)
                        .and("inventory").gte(quantity)
            );
            UpdateResult result = mongoTemplate.updateFirst(
                query,
                new Update().inc("inventory", -quantity),
                Product.class
            );

            if (result.getMatchedCount() == 0) {
                // Atomic operation failed - insufficient inventory RIGHT NOW
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
                insufficient.put(
                    productId,
                    new InventoryInfo(product.getName(), quantity, product.getInventory())
                );
            }
        }

        // P1 FIX: Report all insufficient inventory at once (better UX)
        if (!insufficient.isEmpty()) {
            throw new InsufficientInventoryException(insufficient);
        }
    }
}
