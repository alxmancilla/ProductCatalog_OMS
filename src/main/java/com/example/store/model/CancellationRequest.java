package com.example.store.model;

import lombok.Data;

import java.util.Map;

/**
 * Request model for order cancellation.
 * 
 * Used in the POST /orders/{id}/cancel endpoint.
 */
@Data
public class CancellationRequest {
    
    /**
     * Who is cancelling the order (user ID or email).
     * Required field.
     */
    private String cancelledBy;
    
    /**
     * Reason for cancellation.
     * Optional but recommended for audit purposes.
     * 
     * Examples:
     * - "Customer requested cancellation"
     * - "Out of stock"
     * - "Payment failed"
     * - "Duplicate order"
     */
    private String reason;
    
    /**
     * Optional metadata for the cancellation.
     * 
     * Can include additional information like:
     * - refundId: ID of the refund transaction
     * - customerNote: Note from the customer
     * - supportTicketId: ID of the support ticket
     */
    private Map<String, Object> metadata;
}

