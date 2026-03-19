package com.example.store.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the parsed intent from natural language order request.
 * This is the output of the Intent Parser node.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderIntent {
    
    /**
     * Customer name extracted from the request.
     * Examples: "John Doe", "Jane Smith"
     */
    private String customerName;
    
    /**
     * Customer email if provided.
     */
    private String customerEmail;
    
    /**
     * Customer phone if provided.
     */
    private String customerPhone;
    
    /**
     * List of products requested in the order.
     */
    private List<ProductRequest> products;
    
    /**
     * Represents a single product request in the order.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRequest {
        /**
         * Product name or description from natural language.
         * Examples: "laptop", "blue t-shirt", "MongoDB book"
         */
        private String productDescription;
        
        /**
         * Quantity requested.
         */
        private Integer quantity;
        
        /**
         * Product type hint (if mentioned).
         * Examples: "Electronics", "Clothing", "Book"
         */
        private String typeHint;
        
        /**
         * Additional attributes mentioned (color, size, etc.)
         */
        private String attributes;
    }
}

