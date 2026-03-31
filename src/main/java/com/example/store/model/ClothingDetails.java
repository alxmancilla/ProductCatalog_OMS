package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clothing-specific details for Product.
 * 
 * This is an embedded document (no @Document annotation) that gets stored
 * as a nested object within a Product document.
 * 
 * Example in MongoDB:
 * {
 *   "_id": "prod456",
 *   "type": "Clothing",
 *   "name": "Cotton T-Shirt",
 *   "clothingDetails": {
 *     "size": "L",
 *     "color": "Blue",
 *     "material": "100% Cotton"
 *   }
 * }
 * 
 * Benefits:
 * - Clear separation of type-specific fields
 * - Easy to validate independently
 * - Can be null for non-clothing products
 * - Self-documenting - all clothing fields in one place
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothingDetails {
    
    /**
     * Garment size (e.g., "XS", "S", "M", "L", "XL", "XXL", "32", "38").
     * Required for clothing products.
     */
    private String size;
    
    /**
     * Primary color of the garment (e.g., "Red", "Blue", "Black", "White").
     * Required for clothing products.
     */
    private String color;
    
    /**
     * Fabric composition (e.g., "100% Cotton", "Polyester blend", "Wool").
     * Required for clothing products.
     */
    private String material;
}

