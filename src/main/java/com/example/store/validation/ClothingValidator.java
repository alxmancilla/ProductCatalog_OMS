package com.example.store.validation;

import com.example.store.model.ClothingDetails;
import com.example.store.model.Product;
import org.springframework.stereotype.Component;

/**
 * Validator for Clothing products.
 * 
 * Validates that clothing products have all required fields:
 * - clothingDetails must not be null
 * - size is required and not empty
 * - color is required and not empty
 * - material is required and not empty
 * 
 * Example valid clothing product:
 * {
 *   "type": "Clothing",
 *   "name": "Cotton T-Shirt",
 *   "clothingDetails": {
 *     "size": "L",
 *     "color": "Blue",
 *     "material": "100% Cotton"
 *   }
 * }
 */
@Component
public class ClothingValidator implements ProductValidator {
    
    private static final String TYPE = "Clothing";
    
    @Override
    public void validate(Product product) {
        // Check if clothingDetails object exists
        if (product.getClothingDetails() == null) {
            throw new ProductValidationException(
                TYPE, 
                "clothingDetails", 
                "Clothing products must have clothingDetails object"
            );
        }
        
        ClothingDetails details = product.getClothingDetails();
        
        // Validate size
        if (details.getSize() == null || details.getSize().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "size", 
                "Size is required for clothing products (e.g., 'S', 'M', 'L', 'XL')"
            );
        }
        
        // Validate color
        if (details.getColor() == null || details.getColor().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "color", 
                "Color is required for clothing products (e.g., 'Red', 'Blue', 'Black')"
            );
        }
        
        // Validate material
        if (details.getMaterial() == null || details.getMaterial().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "material", 
                "Material is required for clothing products (e.g., '100% Cotton', 'Polyester blend')"
            );
        }
    }
    
    @Override
    public String getSupportedType() {
        return TYPE;
    }
}

