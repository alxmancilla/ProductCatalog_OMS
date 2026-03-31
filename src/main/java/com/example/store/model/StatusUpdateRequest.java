package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating order status.
 * 
 * This class is used for the PUT /orders/{id}/status endpoint.
 * 
 * Example JSON:
 * {
 *   "newStatus": "CONFIRMED",
 *   "changedBy": "admin@example.com",
 *   "reason": "Payment confirmed",
 *   "metadata": {
 *     "paymentId": "pay_12345",
 *     "amount": 1299.99
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    
    /**
     * The new status to set.
     * Must be a valid OrderStatus value: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
     */
    private OrderStatus newStatus;
    
    /**
     * Who is making this status change.
     * Examples: "system", "admin@example.com", "warehouse-system"
     */
    private String changedBy;
    
    /**
     * Optional reason for the status change.
     * Examples: "Payment confirmed", "Package dispatched", "Customer requested cancellation"
     */
    private String reason;
    
    /**
     * Optional metadata for additional context.
     * Examples:
     * - For SHIPPED: { "trackingNumber": "1Z999AA10123456784", "carrier": "UPS" }
     * - For CANCELLED: { "refundId": "ref_12345", "refundAmount": 1299.99 }
     */
    private Map<String, Object> metadata;
}

