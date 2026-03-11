package com.example.store.controller;

import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderItemBucketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Order operations.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 DEMONSTRATES: COMPUTED PATTERN + OUTLIER PATTERN (Schema Design Patterns)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * COMPUTED PATTERN:
 * - Pre-calculates order total and stores it in the document
 * - Makes queries faster (no recalculation needed)
 *
 * OUTLIER PATTERN:
 * - Handles orders with many items (50+)
 * - Normal orders (< 50 items): Embed items in Order document
 * - Large orders (100+ items): Split into buckets in separate collection
 *
 * Benefits:
 * - Optimizes for the common case (small orders)
 * - Handles outliers gracefully (large orders)
 * - Stays well below MongoDB's 16MB document limit
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemBucketRepository orderItemBucketRepository;

    /**
     * Create a new order.
     * POST /orders
     *
     * 🎯 COMPUTED PATTERN + OUTLIER PATTERN in action:
     * 1. Receives order with items (each has price and quantity)
     * 2. Calculates total = sum of (price × quantity) for all items
     * 3. Checks if this is a large order (50+ items)
     * 4. For large orders (100+ items): Splits into buckets
     * 5. Stores the order (and buckets if needed)
     *
     * Result: Optimized for common case, handles outliers gracefully!
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // Set order date automatically
        order.setOrderDate(LocalDateTime.now());

        // ═══════════════════════════════════════════════════════════════════
        // COMPUTED PATTERN: Calculate and store the total
        // ═══════════════════════════════════════════════════════════════════
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);  // Store the computed value

        // ═══════════════════════════════════════════════════════════════════
        // OUTLIER PATTERN: Handle large orders
        // ═══════════════════════════════════════════════════════════════════
        int itemCount = order.getItems().size();

        if (itemCount >= 50) {
            // Flag as large order
            order.setIsLargeOrder(true);
            order.setTotalItemCount(itemCount);

            // For very large orders (100+ items), use bucketing
            if (itemCount >= 100) {
                return createLargeOrder(order);
            }
            // For 50-99 items, still embed but flag for monitoring
        }

        Order savedOrder = orderRepository.save(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    /**
     * Create a large order with bucketing (100+ items).
     *
     * 🎯 OUTLIER PATTERN: Bucketing Strategy
     * - Splits items into buckets of 50 items each
     * - Stores buckets in separate collection
     * - Main order document contains metadata only
     */
    private ResponseEntity<Order> createLargeOrder(Order order) {
        final int BUCKET_SIZE = 50;
        List<OrderItem> allItems = order.getItems();
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
        }

        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
    
    /**
     * Get all orders.
     * GET /orders
     *
     * Note: For large orders, items are stored in separate buckets.
     * Use GET /orders/{id}/items to retrieve all items for a large order.
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all items for a specific order (handles both normal and large orders).
     * GET /orders/{id}/items
     *
     * 🎯 OUTLIER PATTERN: Retrieval Strategy
     * - For normal orders: Returns embedded items
     * - For large orders: Retrieves and combines all buckets
     */
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Normal order: items are embedded
        if (!order.getIsLargeOrder() || order.getItems() != null) {
            return ResponseEntity.ok(order.getItems());
        }

        // Large order: retrieve from buckets
        List<OrderItemBucket> buckets = orderItemBucketRepository
                .findByOrderIdOrderByBucketNumberAsc(id);

        List<OrderItem> allItems = new ArrayList<>();
        for (OrderItemBucket bucket : buckets) {
            allItems.addAll(bucket.getItems());
        }

        return ResponseEntity.ok(allItems);
    }
}

