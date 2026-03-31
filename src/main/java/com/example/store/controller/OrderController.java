package com.example.store.controller;

import com.example.store.model.CancellationRequest;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.model.OrderStatus;
import com.example.store.model.OrderUpdateRequest;
import com.example.store.model.StatusUpdateRequest;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.service.OrderCancellationService;
import com.example.store.service.OrderStatusService;
import com.example.store.service.OrderTransactionService;
import com.example.store.service.OrderUpdateService;
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

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Autowired
    private OrderStatusService orderStatusService;

    @Autowired
    private OrderCancellationService orderCancellationService;

    @Autowired
    private OrderUpdateService orderUpdateService;

    /**
     * Create a new order with inventory validation and updates.
     * POST /orders
     *
     * 🎯 TRANSACTION + COMPUTED PATTERN + OUTLIER PATTERN in action:
     * 1. Receives order with items (each has price and quantity)
     * 2. Calculates total = sum of (price × quantity) for all items
     * 3. Validates all products exist and have sufficient inventory
     * 4. Creates order and decrements inventory (in a transaction)
     * 5. Checks if this is a large order (50+ items)
     * 6. For large orders (100+ items): Splits into buckets
     *
     * Result: ACID transactions + Optimized for common case + Handles outliers gracefully!
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
     * Get a specific order by ID.
     * GET /orders/{id}
     *
     * 🎯 OUTLIER PATTERN: Handles both normal and large orders
     * - Normal orders (< 100 items): Returns order with embedded items
     * - Large orders (100+ items): Retrieves items from buckets and combines them
     *
     * This is the primary endpoint for retrieving order details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new com.example.store.exception.OrderNotFoundException(id));

        // Handle Outlier Pattern: Retrieve items from buckets if needed
        if (order.getIsLargeOrder() && order.getItems() == null) {
            // Query bucket collection ONLY for large orders (rare case)
            List<OrderItemBucket> buckets = orderItemBucketRepository
                    .findByOrderIdOrderByBucketNumberAsc(id);

            // Combine all bucket items
            List<OrderItem> allItems = new ArrayList<>();
            for (OrderItemBucket bucket : buckets) {
                allItems.addAll(bucket.getItems());
            }

            order.setItems(allItems);
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Get all orders (or filtered by query parameters).
     * GET /orders
     * GET /orders?customerId=xxx
     * GET /orders?status=PENDING (handled by separate endpoint)
     *
     * Note: For large orders, items are stored in separate buckets.
     * Use GET /orders/{id} to retrieve full order details with all items.
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(required = false) String customerId) {

        List<Order> orders;

        if (customerId != null && !customerId.trim().isEmpty()) {
            // Filter by customer
            orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        } else {
            // Get all orders
            orders = orderRepository.findAll();
        }

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

    /**
     * Update order status.
     * PUT /orders/{id}/status
     *
     * 🎯 STATUS MANAGEMENT: Update order lifecycle with validation
     * - Validates status transitions (business rules)
     * - Records change in status history (audit trail)
     * - Updates current status atomically
     *
     * Example request body:
     * {
     *   "newStatus": "CONFIRMED",
     *   "changedBy": "admin@example.com",
     *   "reason": "Payment confirmed",
     *   "metadata": { "paymentId": "pay_12345" }
     * }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestBody StatusUpdateRequest request) {

        Order updatedOrder = orderStatusService.updateOrderStatus(
            id,
            request.getNewStatus(),
            request.getChangedBy(),
            request.getReason(),
            request.getMetadata()
        );

        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Get orders by status.
     * GET /orders?status=PENDING
     *
     * 🎯 STATUS MANAGEMENT: Filter orders by current status
     * - Uses index on status field for fast queries
     * - Returns all orders with the given status
     */
    @GetMapping(params = "status")
    public ResponseEntity<List<Order>> getOrdersByStatus(@RequestParam OrderStatus status) {
        List<Order> orders = orderRepository.findByStatusOrderByOrderDateDesc(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by date range.
     * GET /orders/search/by-date?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     *
     * 🎯 SEARCH & FILTERING: Date range queries
     * - Uses index on orderDate field for fast queries
     * - Returns orders within the specified date range
     */
    @GetMapping("/search/by-date")
    public ResponseEntity<List<Order>> getOrdersByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        List<Order> orders = orderRepository.findByOrderDateBetweenOrderByOrderDateDesc(start, end);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by total amount range.
     * GET /orders/search/by-total?minTotal=100&maxTotal=1000
     *
     * 🎯 SEARCH & FILTERING: Price range queries
     * - Uses index on total field for fast queries
     * - Returns orders within the specified price range
     */
    @GetMapping("/search/by-total")
    public ResponseEntity<List<Order>> getOrdersByTotalRange(
            @RequestParam BigDecimal minTotal,
            @RequestParam BigDecimal maxTotal) {

        List<Order> orders = orderRepository.findByTotalBetween(minTotal, maxTotal);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders for a specific customer with optional status filter.
     * GET /orders/search/by-customer?customerId=xxx
     * GET /orders/search/by-customer?customerId=xxx&status=PENDING
     *
     * 🎯 SEARCH & FILTERING: Customer order history
     * - Uses compound index for optimal performance
     * - Supports optional status filtering
     */
    @GetMapping("/search/by-customer")
    public ResponseEntity<List<Order>> getOrdersByCustomer(
            @RequestParam String customerId,
            @RequestParam(required = false) OrderStatus status) {

        List<Order> orders;

        if (status != null) {
            // Filter by customer AND status
            orders = orderRepository.findByCustomerIdAndStatus(customerId, status);
        } else {
            // Filter by customer only
            orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        }

        return ResponseEntity.ok(orders);
    }

    /**
     * Cancel an order and restore inventory.
     * POST /orders/{id}/cancel
     *
     * 🎯 ORDER CANCELLATION: ACID Transaction with Inventory Restoration
     * - Validates order is in PENDING or CONFIRMED status
     * - Restores inventory for all products atomically
     * - Updates order status to CANCELLED with audit trail
     * - Rollback if any step fails
     *
     * Business Rules:
     * - Can only cancel PENDING or CONFIRMED orders
     * - Cannot cancel PROCESSING, SHIPPED, or DELIVERED orders
     * - All inventory changes are atomic (ACID transaction)
     *
     * Example request body:
     * {
     *   "cancelledBy": "user@example.com",
     *   "reason": "Customer requested cancellation",
     *   "metadata": {
     *     "refundId": "ref_12345",
     *     "supportTicketId": "ticket_67890"
     *   }
     * }
     *
     * @param id The order ID to cancel
     * @param request Cancellation details
     * @return The cancelled order with updated status
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable String id,
            @RequestBody CancellationRequest request) {

        Order cancelledOrder = orderCancellationService.cancelOrder(
            id,
            request.getCancelledBy(),
            request.getReason(),
            request.getMetadata()
        );

        return ResponseEntity.ok(cancelledOrder);
    }

    /**
     * Update order items with automatic inventory delta calculation.
     * PUT /orders/{id}/items
     *
     * 🎯 ORDER UPDATES: ACID Transaction with Inventory Delta Calculation
     * - Validates order is in PENDING status (only modifiable state)
     * - Filters out items with quantity <= 0 (item removal)
     * - Calculates inventory deltas (new - old quantities)
     * - Validates inventory availability for increases
     * - Applies all inventory changes atomically
     * - Recalculates order total (Computed Pattern)
     * - Adds change to order history
     * - Rollback if any step fails
     *
     * Business Rules:
     * - Can only modify PENDING orders
     * - Cannot modify CONFIRMED, PROCESSING, SHIPPED, or DELIVERED orders
     * - Items with quantity 0 are automatically removed from the order
     * - At least one item must remain (use cancellation to remove all items)
     * - All inventory changes are atomic (ACID transaction)
     * - Order total is automatically recalculated
     *
     * Delta Calculation:
     * - Item quantity increased: Decrement inventory
     * - Item quantity decreased: Restore inventory
     * - Item added: Decrement inventory
     * - Item removed (quantity 0): Restore inventory
     *
     * Example 1 - Update quantities:
     * {
     *   "items": [
     *     {
     *       "productId": "prod123",
     *       "name": "Laptop",
     *       "price": 1299.99,
     *       "quantity": 3
     *     },
     *     {
     *       "productId": "prod456",
     *       "name": "Mouse",
     *       "price": 29.99,
     *       "quantity": 1
     *     }
     *   ],
     *   "updatedBy": "customer@example.com",
     *   "reason": "Customer added more items"
     * }
     *
     * Example 2 - Remove item by setting quantity to 0:
     * {
     *   "items": [
     *     {
     *       "productId": "prod123",
     *       "name": "Laptop",
     *       "price": 1299.99,
     *       "quantity": 0
     *     },
     *     {
     *       "productId": "prod456",
     *       "name": "Mouse",
     *       "price": 29.99,
     *       "quantity": 2
     *     }
     *   ],
     *   "updatedBy": "customer@example.com",
     *   "reason": "Customer removed laptop"
     * }
     * Result: Laptop removed, Mouse quantity updated to 2, inventory adjusted accordingly
     *
     * @param id The order ID to update
     * @param request Update details with new items
     * @return The updated order
     */
    @PutMapping("/{id}/items")
    public ResponseEntity<Order> updateOrderItems(
            @PathVariable String id,
            @RequestBody OrderUpdateRequest request) {

        Order updatedOrder = orderUpdateService.updateOrderItems(
            id,
            request.getItems(),
            request.getUpdatedBy(),
            request.getMetadata()
        );

        return ResponseEntity.ok(updatedOrder);
    }
}

