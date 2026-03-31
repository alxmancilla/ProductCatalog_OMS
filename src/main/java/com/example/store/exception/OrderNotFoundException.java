package com.example.store.exception;

/**
 * Exception thrown when an order is not found in the database.
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String orderId) {
        super(String.format("Order not found with ID: %s", orderId));
    }
}

