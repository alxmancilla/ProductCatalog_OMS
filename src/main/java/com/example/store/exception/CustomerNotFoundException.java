package com.example.store.exception;

/**
 * Exception thrown when a customer is not found in the database.
 */
public class CustomerNotFoundException extends RuntimeException {
    
    private final String customerId;
    
    public CustomerNotFoundException(String customerId) {
        super("Customer not found with ID: " + customerId);
        this.customerId = customerId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
}

