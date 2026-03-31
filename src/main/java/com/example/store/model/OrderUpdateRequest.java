package com.example.store.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request model for updating order items.
 * 
 * Used in the PUT /orders/{id}/items endpoint.
 * 
 * This request completely replaces the order's items with the provided list.
 * The service will automatically:
 * 1. Calculate inventory deltas (new - old)
 * 2. Validate inventory availability for increases
 * 3. Apply inventory changes atomically
 * 4. Recalculate order total
 * 5. Add change to order history
 */
@Data
public class OrderUpdateRequest {
    
    /**
     * The new list of items for the order.
     * This completely replaces the existing items.
     * 
     * Each item must include:
     * - productId: ID of the product
     * - name: Product name (snapshot at order time)
     * - price: Product price (snapshot at order time)
     * - quantity: Quantity to order
     * 
     * Required field.
     */
    private List<OrderItem> items;
    
    /**
     * Who is updating the order (user ID or email).
     * Required field.
     */
    private String updatedBy;
    
    /**
     * Optional reason for the update.
     * Recommended for audit purposes.
     * 
     * Examples:
     * - "Customer added more items"
     * - "Customer removed items"
     * - "Customer changed quantities"
     * - "Price adjustment"
     */
    private String reason;
    
    /**
     * Optional metadata for the update.
     * 
     * Can include additional information like:
     * - source: "web" | "mobile" | "admin"
     * - changeType: "add" | "remove" | "modify"
     * - notes: Additional notes about the change
     */
    private Map<String, Object> metadata;
}

