package com.example.store.validation;

import com.example.store.model.Product;

/**
 * Strategy interface for type-specific product validation.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: STRATEGY PATTERN (Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * The Strategy Pattern allows selecting different validation algorithms
 * at runtime based on the product type.
 * 
 * Benefits:
 * - Separation of Concerns: Validation logic separate from data model
 * - Open/Closed Principle: Easy to add new product types without modifying existing code
 * - Single Responsibility: Each validator handles one product type
 * - Testability: Each validator can be tested independently
 * 
 * Usage:
 * ```java
 * ProductValidator validator = validatorFactory.getValidator("Electronics");
 * validator.validate(product); // Throws exception if invalid
 * ```
 * 
 * Implementations:
 * - ElectronicsValidator: Validates electronics-specific fields (warranty, brand)
 * - ClothingValidator: Validates clothing-specific fields (size, color, material)
 * - BookValidator: Validates book-specific fields (author, isbn, pages)
 */
public interface ProductValidator {
    
    /**
     * Validate a product based on type-specific rules.
     * 
     * @param product The product to validate
     * @throws com.example.store.validation.ProductValidationException if validation fails
     */
    void validate(Product product);
    
    /**
     * Get the product type this validator supports.
     * 
     * @return Product type (e.g., "Electronics", "Clothing", "Book")
     */
    String getSupportedType();
}

