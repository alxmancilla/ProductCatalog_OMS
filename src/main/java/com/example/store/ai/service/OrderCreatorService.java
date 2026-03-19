package com.example.store.ai.service;

import com.example.store.ai.model.AgentState;
import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.service.OrderTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for creating orders from resolved products and customer.
 * Implements the Computed Pattern, Outlier Pattern, and Transactions.
 */
@Service
public class OrderCreatorService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatorService.class);
    private static final int LARGE_ORDER_THRESHOLD = 50;
    private static final int BUCKETING_THRESHOLD = 100;
    private static final int BUCKET_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderItemBucketRepository orderItemBucketRepository;
    private final OrderTransactionService orderTransactionService;

    @Autowired
    public OrderCreatorService(
            OrderRepository orderRepository,
            OrderItemBucketRepository orderItemBucketRepository,
            OrderTransactionService orderTransactionService) {
        this.orderRepository = orderRepository;
        this.orderItemBucketRepository = orderItemBucketRepository;
        this.orderTransactionService = orderTransactionService;
    }
    
    /**
     * Create an order from resolved products and customer.
     * Uses transactions to ensure inventory is validated and updated atomically.
     *
     * @param customer The customer placing the order
     * @param resolvedProducts List of resolved products with quantities
     * @return The created order
     * @throws InsufficientInventoryException if any product has insufficient inventory
     */
    public Order createOrder(Customer customer, List<AgentState.ResolvedProduct> resolvedProducts) {
        if (resolvedProducts == null || resolvedProducts.isEmpty()) {
            throw new IllegalArgumentException("Cannot create order without products");
        }

        // Create order items
        List<OrderItem> items = new ArrayList<>();
        for (AgentState.ResolvedProduct resolved : resolvedProducts) {
            OrderItem item = new OrderItem();
            item.setProductId(resolved.getProduct().getId());
            item.setName(resolved.getProduct().getName());
            item.setPrice(resolved.getProduct().getPrice());
            item.setQuantity(resolved.getQuantity());
            items.add(item);
        }

        // Create order
        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setOrderDate(LocalDateTime.now());
        order.setSchemaVersion(3);

        // COMPUTED PATTERN: Calculate total
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        // OUTLIER PATTERN: Handle large orders
        int itemCount = items.size();

        if (itemCount >= LARGE_ORDER_THRESHOLD) {
            order.setIsLargeOrder(true);
            order.setTotalItemCount(itemCount);

            if (itemCount >= BUCKETING_THRESHOLD) {
                // Very large order - use bucketing
                logger.info("Creating large order with {} items (bucketing)", itemCount);
                return createLargeOrderWithBuckets(order, items);
            } else {
                // Large but not huge - still embed items
                logger.info("Creating large order with {} items (embedded)", itemCount);
                order.setItems(items);
            }
        } else {
            // Normal order - embed items
            logger.info("Creating normal order with {} items", itemCount);
            order.setIsLargeOrder(false);
            order.setItems(items);
        }

        // ═══════════════════════════════════════════════════════════════════
        // TRANSACTION: Create order and update inventory atomically
        // ═══════════════════════════════════════════════════════════════════
        // This will:
        // 1. Validate all products exist
        // 2. Check inventory availability
        // 3. Create the order
        // 4. Decrement inventory for all products
        // 5. Rollback everything if any step fails
        Order savedOrder = orderTransactionService.createOrderWithInventoryUpdate(order);
        logger.info("Created order {} for customer {} with total ${}",
                savedOrder.getId(), customer.getName(), total);

        return savedOrder;
    }
    
    /**
     * Create a large order with bucketing (100+ items).
     * Implements the Outlier Pattern.
     */
    private Order createLargeOrderWithBuckets(Order order, List<OrderItem> allItems) {
        int totalItems = allItems.size();
        int bucketCount = (int) Math.ceil((double) totalItems / BUCKET_SIZE);
        
        // Update order metadata
        order.setTotalItemCount(totalItems);
        order.setBucketCount(bucketCount);
        order.setItems(null);  // Don't embed items for large orders
        
        // Save the main order document (without items)
        Order savedOrder = orderRepository.save(order);
        
        // Create and save buckets
        for (int i = 0; i < bucketCount; i++) {
            int startIndex = i * BUCKET_SIZE;
            int endIndex = Math.min(startIndex + BUCKET_SIZE, totalItems);
            List<OrderItem> bucketItems = allItems.subList(startIndex, endIndex);
            
            OrderItemBucket bucket = new OrderItemBucket();
            bucket.setId(savedOrder.getId() + "_bucket_" + i);
            bucket.setOrderId(savedOrder.getId());
            bucket.setBucketNumber(i);
            bucket.setItems(bucketItems);
            
            orderItemBucketRepository.save(bucket);
            logger.debug("Created bucket {} with {} items", i, bucketItems.size());
        }
        
        logger.info("Created large order with {} buckets", bucketCount);
        return savedOrder;
    }
}

