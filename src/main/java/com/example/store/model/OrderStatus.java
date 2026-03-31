package com.example.store.model;

/**
 * Enum representing the lifecycle status of an order.
 * 
 * Status Flow:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *    ↓
 * CANCELLED (from PENDING or CONFIRMED only)
 * 
 * Business Rules:
 * - New orders start as PENDING
 * - Can only cancel PENDING or CONFIRMED orders
 * - Cannot move backwards in the flow (except to CANCELLED)
 * - DELIVERED and CANCELLED are terminal states
 */
public enum OrderStatus {
    /**
     * Order created but not yet confirmed.
     * Can be cancelled or modified.
     */
    PENDING,
    
    /**
     * Order confirmed by customer/admin.
     * Payment typically authorized at this stage.
     * Can still be cancelled.
     */
    CONFIRMED,
    
    /**
     * Order is being prepared/picked.
     * Cannot be cancelled (contact support).
     */
    PROCESSING,
    
    /**
     * Order has been shipped to customer.
     * Tracking number should be available.
     * Cannot be cancelled (must be returned).
     */
    SHIPPED,
    
    /**
     * Order delivered to customer.
     * Terminal state - can only request return.
     */
    DELIVERED,
    
    /**
     * Order cancelled by customer or admin.
     * Terminal state - inventory restored.
     */
    CANCELLED
}

