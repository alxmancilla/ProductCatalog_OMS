package com.example.store.exception;

import java.util.Map;

/**
 * Exception thrown when there is insufficient inventory to fulfill an order.
 * Contains details about which products have insufficient stock.
 */
public class InsufficientInventoryException extends RuntimeException {
    
    private final Map<String, InventoryInfo> insufficientProducts;
    
    /**
     * Information about insufficient inventory for a product
     */
    public static class InventoryInfo {
        private final String productName;
        private final int requested;
        private final int available;
        
        public InventoryInfo(String productName, int requested, int available) {
            this.productName = productName;
            this.requested = requested;
            this.available = available;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public int getRequested() {
            return requested;
        }
        
        public int getAvailable() {
            return available;
        }
    }
    
    public InsufficientInventoryException(Map<String, InventoryInfo> insufficientProducts) {
        super("Insufficient inventory for one or more products");
        this.insufficientProducts = insufficientProducts;
    }
    
    public Map<String, InventoryInfo> getInsufficientProducts() {
        return insufficientProducts;
    }
}

