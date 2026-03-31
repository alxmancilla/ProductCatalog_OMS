package com.example.store.validation;

/**
 * Exception thrown when product type-specific validation fails.
 * 
 * This exception provides detailed error messages about what validation
 * rule was violated and for which product type.
 */
public class ProductValidationException extends RuntimeException {
    
    private final String productType;
    private final String fieldName;
    
    /**
     * Create a validation exception with a detailed message.
     * 
     * @param message Error message describing the validation failure
     */
    public ProductValidationException(String message) {
        super(message);
        this.productType = null;
        this.fieldName = null;
    }
    
    /**
     * Create a validation exception with product type and field information.
     * 
     * @param productType The type of product that failed validation
     * @param fieldName The field that failed validation
     * @param message Error message describing the validation failure
     */
    public ProductValidationException(String productType, String fieldName, String message) {
        super(String.format("[%s] %s: %s", productType, fieldName, message));
        this.productType = productType;
        this.fieldName = fieldName;
    }
    
    public String getProductType() {
        return productType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
}

