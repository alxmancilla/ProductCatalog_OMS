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
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
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
 * 🎯 PURPOSE: ACID Transactions for Order Creation + Inventory Updates
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This service ensures that:
 * 1. Customer exists
 * 2. All products exist and have sufficient inventory
 * 3. Order is created successfully
 * 4. Inventory is decremented for all products atomically
 * 5. If ANY step fails, EVERYTHING is rolled back
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
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Create a normal order (< 100 items) with inventory validation and updates in a transaction.
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
        validateInventory(order.getItems());

        Order savedOrder = orderRepository.save(order);

        decrementInventory(order.getItems());

        return savedOrder;
    }

    /**
     * Create a large order (100+ items) with inventory validation in a transaction.
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
        validateInventory(items);

        // Save the order header (items are stored in buckets, not embedded)
        Order savedOrder = orderRepository.save(order);

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
     * Validate that all products exist and have sufficient inventory.
     * Throws an exception listing ALL insufficient products at once (better UX).
     */
    private void validateInventory(List<OrderItem> items) {
        Map<String, InventoryInfo> insufficient = new HashMap<>();

        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            if (product.getInventory() < item.getQuantity()) {
                insufficient.put(
                    item.getProductId(),
                    new InventoryInfo(product.getName(), item.getQuantity(), product.getInventory())
                );
            }
        }

        if (!insufficient.isEmpty()) {
            throw new InsufficientInventoryException(insufficient);
        }
    }

    /**
     * Atomically decrement inventory for each item using MongoDB's $inc operator.
     *
     * The query filter includes .and("inventory").gte(quantity) so the update
     * only succeeds if inventory is still sufficient. This prevents a race
     * condition where two concurrent requests both pass validateInventory()
     * but only one should succeed.
     *
     * If the atomic update finds no match (inventory dropped between validation
     * and update), an InsufficientInventoryException is thrown and the
     * surrounding @Transactional rolls back all prior changes.
     */
    private void decrementInventory(List<OrderItem> items) {
        for (OrderItem item : items) {
            String productId = item.getProductId();
            int quantity = item.getQuantity();

            // ═══════════════════════════════════════════════════════════════
            // ATOMIC CONDITIONAL UPDATE:
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
                // Inventory dropped between validation and update (race condition caught)
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
                throw new InsufficientInventoryException(Map.of(
                    productId,
                    new InventoryInfo(product.getName(), quantity, product.getInventory())
                ));
            }
        }
    }
}
