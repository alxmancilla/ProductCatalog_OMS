package com.example.store.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from the AI Order Assistant.
 * This is what gets returned to the user via the REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    
    /**
     * Natural language response message.
     */
    private String message;
    
    /**
     * Whether the order was successfully created.
     */
    private boolean success;
    
    /**
     * The created order ID (if successful).
     */
    private String orderId;
    
    /**
     * Customer ID associated with the order.
     */
    private String customerId;
    
    /**
     * Customer name.
     */
    private String customerName;
    
    /**
     * Total order amount.
     */
    private String total;
    
    /**
     * Number of items in the order.
     */
    private Integer itemCount;
    
    /**
     * Error message (if failed).
     */
    private String error;
}

