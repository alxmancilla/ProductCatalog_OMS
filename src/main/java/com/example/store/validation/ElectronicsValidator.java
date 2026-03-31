package com.example.store.validation;

import com.example.store.model.ElectronicsDetails;
import com.example.store.model.Product;
import org.springframework.stereotype.Component;

/**
 * Validator for Electronics products.
 * 
 * Validates that electronics products have all required fields:
 * - electronicsDetails must not be null
 * - warranty is required and not empty
 * - brand is required and not empty
 * 
 * Example valid electronics product:
 * {
 *   "type": "Electronics",
 *   "name": "Laptop Pro 15",
 *   "electronicsDetails": {
 *     "warranty": "2 years",
 *     "brand": "TechCorp",
 *     "weight": "1.8 kg",         // optional
 *     "screenSize": "15.6 inch"   // optional
 *   }
 * }
 */
@Component
public class ElectronicsValidator implements ProductValidator {
    
    private static final String TYPE = "Electronics";
    
    @Override
    public void validate(Product product) {
        // Check if electronicsDetails object exists
        if (product.getElectronicsDetails() == null) {
            throw new ProductValidationException(
                TYPE, 
                "electronicsDetails", 
                "Electronics products must have electronicsDetails object"
            );
        }
        
        ElectronicsDetails details = product.getElectronicsDetails();
        
        // Validate warranty
        if (details.getWarranty() == null || details.getWarranty().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "warranty", 
                "Warranty is required for electronics products (e.g., '1 year', '2 years')"
            );
        }
        
        // Validate brand
        if (details.getBrand() == null || details.getBrand().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "brand", 
                "Brand is required for electronics products (e.g., 'TechCorp', 'Samsung')"
            );
        }
        
        // Optional fields (weight, screenSize, resolution, capacity) don't need validation
        // They can be null or empty for products that don't have those attributes
    }
    
    @Override
    public String getSupportedType() {
        return TYPE;
    }
}

