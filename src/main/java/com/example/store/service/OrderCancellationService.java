package com.example.store.service;

import com.example.store.exception.OrderNotFoundException;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.model.OrderStatus;
import com.example.store.model.StatusChange;
import com.example.store.model.Product;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling order cancellations with inventory restoration.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: ACID Transactions for Order Cancellation + Inventory Restoration
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service ensures that:
 * 1. Order exists and is in a cancellable state (PENDING or CONFIRMED)
 * 2. Inventory is restored for all products (reverse of order creation)
 * 3. Order status is updated to CANCELLED with audit trail
 * 4. If ANY step fails, EVERYTHING is rolled back
 *
 * Transaction Flow:
 * ┌─────────────────────────────────────────┐
 * │ START TRANSACTION                       │
 * ├─────────────────────────────────────────┤
 * │ 1. Validate order exists                │
 * │ 2. Validate order is cancellable        │
 * │ 3. Retrieve all order items             │
 * │ 4. Restore inventory for each product   │
 * │ 5. Update order status to CANCELLED     │
 * │ 6. Add status change to history         │
 * ├─────────────────────────────────────────┤
 * │ COMMIT (if all succeed)                 │
 * │ ROLLBACK (if any fail)                  │
 * └─────────────────────────────────────────┘
 *
 * IMPORTANT: Requires MongoDB to be running as a replica set!
 */
@Service
public class OrderCancellationService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemBucketRepository orderItemBucketRepository;

    /**
     * Cancel an order and restore inventory atomically.
     *
     * @param orderId The order ID to cancel
     * @param cancelledBy Who is cancelling the order
     * @param reason Reason for cancellation
     * @return The cancelled order
     * @throws OrderNotFoundException if order not found
     * @throws IllegalStateException if order cannot be cancelled
     * @throws ProductNotFoundException if any product is not found
     */
    @Transactional
    public Order cancelOrder(String orderId, String cancelledBy, String reason) {
        return cancelOrder(orderId, cancelledBy, reason, null);
    }

    /**
     * Cancel an order and restore inventory atomically with metadata.
     *
     * @param orderId The order ID to cancel
     * @param cancelledBy Who is cancelling the order
     * @param reason Reason for cancellation
     * @param metadata Optional metadata for the cancellation
     * @return The cancelled order
     * @throws OrderNotFoundException if order not found
     * @throws IllegalStateException if order cannot be cancelled
     * @throws ProductNotFoundException if any product is not found
     */
    @Transactional
    public Order cancelOrder(String orderId, String cancelledBy, String reason, 
                            Map<String, Object> metadata) {
        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Validate order exists and is cancellable
        // ═══════════════════════════════════════════════════════════════════
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Business rule: Can only cancel PENDING or CONFIRMED orders
        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot cancel order in " + order.getStatus() + " status. " +
                "Only PENDING and CONFIRMED orders can be cancelled.");
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Restore inventory for all items
        // ═══════════════════════════════════════════════════════════════════
        List<OrderItem> items = getOrderItems(order);  // Handle Outlier Pattern

        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            // Restore inventory (ADD back what was decremented during order creation)
            int restoredInventory = product.getInventory() + item.getQuantity();
            product.setInventory(restoredInventory);

            productRepository.save(product);
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Update order status with history
        // ═══════════════════════════════════════════════════════════════════
        StatusChange change = new StatusChange();
        change.setFromStatus(order.getStatus());
        change.setToStatus(OrderStatus.CANCELLED);
        change.setChangedAt(LocalDateTime.now());
        change.setChangedBy(cancelledBy);
        change.setReason(reason);
        change.setMetadata(metadata != null ? metadata : new HashMap<>());

        order.setStatus(OrderStatus.CANCELLED);
        order.getStatusHistory().add(change);

        Order cancelledOrder = orderRepository.save(order);

        // ═══════════════════════════════════════════════════════════════════
        // TRANSACTION COMMITS HERE!
        // If any step failed, MongoDB rolls back EVERYTHING:
        // - Inventory changes reverted
        // - Order status unchanged
        // ═══════════════════════════════════════════════════════════════════

        return cancelledOrder;
    }

    /**
     * Helper method to get all items for an order (handles Outlier Pattern).
     * 
     * - For normal orders: Returns embedded items
     * - For large orders: Retrieves and combines all buckets
     */
    private List<OrderItem> getOrderItems(Order order) {
        if (!order.getIsLargeOrder() || order.getItems() != null) {
            return order.getItems();
        }

        // Large order - retrieve from buckets
        List<OrderItemBucket> buckets = orderItemBucketRepository
            .findByOrderIdOrderByBucketNumberAsc(order.getId());

        List<OrderItem> allItems = new ArrayList<>();
        for (OrderItemBucket bucket : buckets) {
            allItems.addAll(bucket.getItems());
        }

        return allItems;
    }
}

