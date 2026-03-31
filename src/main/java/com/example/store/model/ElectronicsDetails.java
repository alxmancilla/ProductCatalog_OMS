package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Electronics-specific details for Product.
 * 
 * This is an embedded document (no @Document annotation) that gets stored
 * as a nested object within a Product document.
 * 
 * Example in MongoDB:
 * {
 *   "_id": "prod123",
 *   "type": "Electronics",
 *   "name": "Laptop Pro 15",
 *   "electronicsDetails": {
 *     "warranty": "2 years",
 *     "brand": "TechCorp",
 *     "weight": "1.8 kg",
 *     "screenSize": "15.6 inch",
 *     "resolution": "Full HD",
 *     "capacity": "512GB SSD"
 *   }
 * }
 * 
 * Benefits:
 * - Clear separation of type-specific fields
 * - Easy to validate independently
 * - Can be null for non-electronics products
 * - Self-documenting - all electronics fields in one place
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicsDetails {
    
    /**
     * Product warranty period (e.g., "1 year", "2 years", "90 days").
     * Required for electronics products.
     */
    private String warranty;
    
    /**
     * Manufacturer or brand name (e.g., "TechCorp", "Samsung", "Apple").
     * Required for electronics products.
     */
    private String brand;
    
    /**
     * Physical weight of the product (e.g., "1.8 kg", "250g", "3.5 lbs").
     * Optional - mainly for laptops, tablets, and larger electronics.
     */
    private String weight;
    
    /**
     * Screen size for devices with displays (e.g., "15.6 inch", "27 inch", "6.5 inch").
     * Optional - for laptops, monitors, tablets, phones.
     */
    private String screenSize;
    
    /**
     * Display or camera resolution (e.g., "4K", "Full HD", "1080p", "12MP").
     * Optional - for displays, cameras, monitors.
     */
    private String resolution;
    
    /**
     * Storage or battery capacity (e.g., "512GB SSD", "1TB HDD", "5000mAh").
     * Optional - for storage devices, laptops, phones, power banks.
     */
    private String capacity;
}

