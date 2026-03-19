package com.example.store.exception;

/**
 * Exception thrown when a product is not found in the database.
 */
public class ProductNotFoundException extends RuntimeException {
    
    private final String productId;
    
    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
        this.productId = productId;
    }
    
    public String getProductId() {
        return productId;
    }
}

