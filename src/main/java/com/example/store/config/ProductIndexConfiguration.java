package com.example.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import jakarta.annotation.PostConstruct;

/**
 * Configuration class for creating MongoDB indexes on the products collection.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Performance Optimization for Product Queries (P1 FIX)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This class creates indexes to support fast queries for:
 * - Product lookup by SKU (unique identifier)
 * - Product search by category
 * - Price range filtering
 * - Product type filtering
 * - Combined category + price queries
 * 
 * MongoDB Best Practices:
 * - Indexes are created at application startup
 * - Compound indexes support multiple query patterns
 * - Unique constraint on SKU prevents duplicates
 * - Existing indexes are not recreated (idempotent)
 */
@Configuration
@RequiredArgsConstructor
public class ProductIndexConfiguration {

    private final MongoTemplate mongoTemplate;

    /**
     * Create indexes after application context is initialized.
     * This runs once at startup.
     */
    @PostConstruct
    public void initIndexes() {
        createProductIndexes();
    }

    /**
     * Create all required indexes for the products collection.
     * 
     * P1 Fix: Add SKU index for findBySku() performance
     * 
     * Performance Impact:
     * - Without SKU index: O(n) table scan
     * - With SKU index: O(log n) or O(1) with unique constraint
     * 
     * Example:
     * - 10,000 products without index: ~50ms
     * - 10,000 products with index: <1ms
     */
    private void createProductIndexes() {
        String collectionName = "products";

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 1: SKU - Unique identifier (P1 FIX)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findBySku()
        //           Product lookup by SKU
        // 
        // Why unique:
        // - SKU (Stock Keeping Unit) must be unique per product
        // - Prevents accidental duplicate entries
        // - Enables O(1) lookups (B-tree index)
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("sku", Sort.Direction.ASC)
                .unique()
                .named("idx_sku_unique"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 2: Category for filtering
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByCategory()
        //           GET /products/search/by-category?category=Electronics
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .named("idx_category_name"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 3: Product type (polymorphic pattern support)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByProductType()
        //           GET /products/search/by-type?type=ELECTRONICS
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("productType", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .named("idx_productType_name"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 4: Price range queries
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByPriceBetween()
        //           GET /products/search/by-price?minPrice=100&maxPrice=500
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("price", Sort.Direction.ASC)
                .named("idx_price"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 5: Inventory tracking (for low-stock alerts)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByInventoryLessThan()
        //           GET /products/search/low-stock?threshold=10
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("inventory", Sort.Direction.ASC)
                .named("idx_inventory"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 6: Category + Price compound query
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByCategoryAndPriceBetween()
        //           GET /products/search?category=Electronics&minPrice=100
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .on("price", Sort.Direction.ASC)
                .named("idx_category_price"));

        System.out.println("✅ Product indexes created successfully!");
        System.out.println("   - idx_sku_unique (P1 FIX)");
        System.out.println("   - idx_category_name");
        System.out.println("   - idx_productType_name");
        System.out.println("   - idx_price");
        System.out.println("   - idx_inventory");
        System.out.println("   - idx_category_price");
    }
}
