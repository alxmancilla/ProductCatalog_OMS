package com.example.store.service;

import com.example.store.model.Product;
import com.example.store.validation.ProductValidationException;
import com.example.store.validation.ProductValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for orchestrating type-specific product validation using Strategy Pattern.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: STRATEGY PATTERN (Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service acts as a "context" in the Strategy Pattern, delegating
 * validation to the appropriate strategy (validator) based on product type.
 * 
 * Flow:
 * 1. Product arrives with a "type" field (e.g., "Electronics")
 * 2. Service looks up the appropriate validator from the registry
 * 3. Validator performs type-specific validation
 * 4. If validation fails, ProductValidationException is thrown
 * 
 * Benefits:
 * - Single entry point for all product validation
 * - Automatic validator discovery via Spring dependency injection
 * - Easy to add new product types - just create a new validator
 * - Type-specific validation rules are encapsulated in validators
 * 
 * Usage in Controller:
 * ```java
 * @PostMapping
 * public Product createProduct(@RequestBody Product product) {
 *     productValidationService.validate(product);  // Validates based on type
 *     return productRepository.save(product);
 * }
 * ```
 */
@Service
public class ProductValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductValidationService.class);
    
    private final Map<String, ProductValidator> validators = new HashMap<>();
    
    /**
     * Constructor that auto-discovers all ProductValidator beans.
     * 
     * Spring automatically injects all beans that implement ProductValidator,
     * and we register them in a map for quick lookup by product type.
     * 
     * @param validatorList List of all ProductValidator beans from Spring context
     */
    @Autowired
    public ProductValidationService(List<ProductValidator> validatorList) {
        for (ProductValidator validator : validatorList) {
            validators.put(validator.getSupportedType(), validator);
            logger.info("✅ Registered validator for product type: {}", validator.getSupportedType());
        }
        
        logger.info("📋 Product validation service initialized with {} validators", validators.size());
    }
    
    /**
     * Validate a product using the appropriate type-specific validator.
     *
     * @param product The product to validate
     * @throws ProductValidationException if validation fails
     */
    public void validate(Product product) {
        // Check if product has a type
        if (product.getType() == null || product.getType().trim().isEmpty()) {
            // No type specified - treat as generic product, skip type-specific validation
            logger.info("ℹ️  Product '{}' has no type specified. Treating as generic product.", product.getName());
            return;
        }

        String type = product.getType();

        // Special handling for "General" type (generic products)
        if ("General".equals(type)) {
            logger.info("ℹ️  Product '{}' is type 'General'. Skipping type-specific validation.", product.getName());
            return;
        }

        // Look up the appropriate validator
        ProductValidator validator = validators.get(type);

        if (validator == null) {
            // Unknown product type - log warning but allow it
            // This supports adding new product types without updating code
            logger.warn("⚠️  No validator found for product type '{}'. Skipping type-specific validation.", type);
            return;
        }

        // Delegate to the type-specific validator
        logger.debug("🔍 Validating {} product: {}", type, product.getName());
        validator.validate(product);
        logger.debug("✅ Validation passed for {} product: {}", type, product.getName());
    }
    
    /**
     * Check if a validator exists for the given product type.
     * 
     * @param productType The product type to check
     * @return true if a validator exists, false otherwise
     */
    public boolean hasValidator(String productType) {
        return validators.containsKey(productType);
    }
    
    /**
     * Get all supported product types.
     * 
     * @return Set of product type names that have validators
     */
    public java.util.Set<String> getSupportedTypes() {
        return validators.keySet();
    }
}

