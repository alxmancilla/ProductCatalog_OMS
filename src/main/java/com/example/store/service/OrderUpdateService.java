package com.example.store.service;

import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.exception.InvalidOrderStateException;
import com.example.store.exception.OrderNotFoundException;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.model.OrderStatus;
import com.example.store.model.Product;
import com.example.store.model.StatusChange;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling order updates with inventory delta calculation.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: ACID Transactions for Order Updates + Inventory Delta Management
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service ensures that:
 * 1. Order exists and is in PENDING status (only modifiable state)
 * 2. Inventory deltas are calculated correctly (new - old)
 * 3. Inventory availability is validated for increases
 * 4. All inventory changes are applied atomically
 * 5. Order total is recalculated (Computed Pattern)
 * 6. If ANY step fails, EVERYTHING is rolled back
 *
 * Transaction Flow:
 * ┌─────────────────────────────────────────┐
 * │ START TRANSACTION                       │
 * ├─────────────────────────────────────────┤
 * │ 1. Validate order exists                │
 * │ 2. Validate order is PENDING            │
 * │ 3. Get old items (handle large orders)  │
 * │ 4. Calculate inventory deltas           │
 * │ 5. Validate inventory for increases     │
 * │ 6. Apply inventory changes              │
 * │ 7. Update order items                   │
 * │ 8. Recalculate order total              │
 * │ 9. Add change to history                │
 * ├─────────────────────────────────────────┤
 * │ COMMIT (if all succeed)                 │
 * │ ROLLBACK (if any fail)                  │
 * └─────────────────────────────────────────┘
 *
 * Delta Calculation Examples:
 * - Old: Laptop(2), Mouse(3)
 * - New: Laptop(3), Mouse(1), Keyboard(1)
 * - Deltas:
 *   - Laptop: +1 (decrement inventory by 1)
 *   - Mouse: -2 (restore inventory by 2)
 *   - Keyboard: +1 (decrement inventory by 1)
 *
 * Removing Items (Quantity 0):
 * - Items with quantity 0 are automatically filtered out (removed from order)
 * - Old: Laptop(2), Mouse(3)
 * - New: Laptop(0), Mouse(5)
 * - Result: Laptop removed, Mouse quantity updated to 5
 * - Deltas: Laptop: -2 (restore 2 to inventory), Mouse: +2 (decrement 2 from inventory)
 *
 * IMPORTANT: Requires MongoDB to be running as a replica set!
 */
@Service
public class OrderUpdateService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemBucketRepository orderItemBucketRepository;

    /**
     * Update order items and adjust inventory atomically.
     *
     * @param orderId The order ID to update
     * @param newItems The new list of items (replaces old items)
     * @param updatedBy Who is updating the order
     * @return The updated order
     * @throws OrderNotFoundException if order not found
     * @throws InvalidOrderStateException if order is not in PENDING status
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientInventoryException if insufficient inventory for increases
     */
    @Transactional
    public Order updateOrderItems(String orderId, List<OrderItem> newItems, String updatedBy) {
        return updateOrderItems(orderId, newItems, updatedBy, null);
    }

    /**
     * Update order items and adjust inventory atomically with metadata.
     *
     * @param orderId The order ID to update
     * @param newItems The new list of items (replaces old items)
     * @param updatedBy Who is updating the order
     * @param metadata Optional metadata for the update
     * @return The updated order
     * @throws OrderNotFoundException if order not found
     * @throws InvalidOrderStateException if order is not in PENDING status
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientInventoryException if insufficient inventory for increases
     */
    @Transactional
    public Order updateOrderItems(String orderId, List<OrderItem> newItems, String updatedBy,
                                  Map<String, Object> metadata) {
        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Validate order exists and is in PENDING status
        // ═══════════════════════════════════════════════════════════════════
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Business rule: Can only modify PENDING orders
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                "Can only modify orders in PENDING status. Current status: " + order.getStatus());
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 1.5: Filter out items with quantity 0 (removing items)
        // ═══════════════════════════════════════════════════════════════════
        // Items with quantity 0 should be removed from the order
        List<OrderItem> filteredNewItems = newItems.stream()
            .filter(item -> item.getQuantity() > 0)
            .collect(Collectors.toList());

        // Validate at least one item remains
        if (filteredNewItems.isEmpty()) {
            throw new IllegalArgumentException(
                "Order must have at least one item with quantity > 0. Use cancellation to remove all items.");
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Calculate inventory deltas (NEW - OLD)
        // ═══════════════════════════════════════════════════════════════════
        List<OrderItem> oldItems = getOrderItems(order);
        Map<String, Integer> inventoryDeltas = calculateInventoryDeltas(oldItems, filteredNewItems);

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Validate inventory availability for increases
        // ═══════════════════════════════════════════════════════════════════
        Map<String, InventoryInfo> insufficientProducts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : inventoryDeltas.entrySet()) {
            String productId = entry.getKey();
            int delta = entry.getValue();

            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            // Positive delta means quantity increased (need more inventory)
            if (delta > 0 && product.getInventory() < delta) {
                insufficientProducts.put(
                    productId,
                    new InventoryInfo(
                        product.getName(),
                        delta,
                        product.getInventory()
                    )
                );
            }
        }

        // If any products have insufficient inventory, throw exception
        if (!insufficientProducts.isEmpty()) {
            throw new InsufficientInventoryException(insufficientProducts);
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 4: Apply inventory changes
        // ═══════════════════════════════════════════════════════════════════
        // Delta > 0: Quantity increased → Decrement inventory
        // Delta < 0: Quantity decreased → Restore inventory
        for (Map.Entry<String, Integer> entry : inventoryDeltas.entrySet()) {
            String productId = entry.getKey();
            int delta = entry.getValue();

            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            // Apply delta: subtract from inventory (positive delta = more items needed)
            int newInventory = product.getInventory() - delta;
            product.setInventory(newInventory);

            productRepository.save(product);
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 5: Update order items and recalculate total
        // ═══════════════════════════════════════════════════════════════════
        order.setItems(filteredNewItems);

        // Computed Pattern: Recalculate total
        BigDecimal newTotal = filteredNewItems.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(newTotal);

        // Update large order flags based on new item count
        int newItemCount = filteredNewItems.size();
        order.setIsLargeOrder(newItemCount >= 50);
        if (newItemCount >= 50) {
            order.setTotalItemCount(newItemCount);
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 6: Add modification to history
        // ═══════════════════════════════════════════════════════════════════
        StatusChange change = new StatusChange();
        change.setFromStatus(order.getStatus());
        change.setToStatus(order.getStatus());  // Status stays PENDING
        change.setChangedAt(LocalDateTime.now());
        change.setChangedBy(updatedBy);
        change.setReason("Order items modified");

        Map<String, Object> changeMetadata = new HashMap<>();
        changeMetadata.put("action", "items_updated");
        changeMetadata.put("oldItemCount", oldItems.size());
        changeMetadata.put("newItemCount", newItems.size());
        changeMetadata.put("oldTotal", order.getTotal());  // Will be old value before save
        changeMetadata.put("newTotal", newTotal);

        if (metadata != null) {
            changeMetadata.putAll(metadata);
        }

        change.setMetadata(changeMetadata);
        order.getStatusHistory().add(change);

        // Save the updated order
        Order updatedOrder = orderRepository.save(order);

        // ═══════════════════════════════════════════════════════════════════
        // TRANSACTION COMMITS HERE!
        // If any step failed, MongoDB rolls back EVERYTHING:
        // - Inventory changes reverted
        // - Order items unchanged
        // ═══════════════════════════════════════════════════════════════════

        return updatedOrder;
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

    /**
     * Calculate inventory deltas: Map<ProductId, QuantityDelta>
     *
     * Delta = New Quantity - Old Quantity
     * - Positive delta: Need to decrement inventory (quantity increased)
     * - Negative delta: Need to restore inventory (quantity decreased)
     * - Zero delta: No change (not included in result)
     *
     * Examples:
     * - Old: Laptop(2), Mouse(3)
     * - New: Laptop(3), Mouse(1), Keyboard(1)
     * - Deltas: { Laptop: +1, Mouse: -2, Keyboard: +1 }
     */
    private Map<String, Integer> calculateInventoryDeltas(
            List<OrderItem> oldItems, List<OrderItem> newItems) {

        Map<String, Integer> deltas = new HashMap<>();

        // Create maps for easy lookup: ProductId -> Quantity
        Map<String, Integer> oldQty = oldItems.stream()
            .collect(Collectors.toMap(
                OrderItem::getProductId,
                OrderItem::getQuantity));

        Map<String, Integer> newQty = newItems.stream()
            .collect(Collectors.toMap(
                OrderItem::getProductId,
                OrderItem::getQuantity));

        // Find all affected products (union of old and new)
        Set<String> allProductIds = new HashSet<>();
        allProductIds.addAll(oldQty.keySet());
        allProductIds.addAll(newQty.keySet());

        // Calculate delta for each product
        for (String productId : allProductIds) {
            int oldQuantity = oldQty.getOrDefault(productId, 0);
            int newQuantity = newQty.getOrDefault(productId, 0);
            int delta = newQuantity - oldQuantity;

            // Only include products that changed
            if (delta != 0) {
                deltas.put(productId, delta);
            }
        }

        return deltas;
    }
}


