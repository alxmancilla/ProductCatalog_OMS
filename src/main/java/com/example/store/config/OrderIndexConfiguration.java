package com.example.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import jakarta.annotation.PostConstruct;

/**
 * Configuration class for creating MongoDB indexes on the orders collection.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Performance Optimization for Order Queries
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This class creates indexes to support fast queries for:
 * - Order retrieval by ID (automatic _id index)
 * - Customer order history (customerId + orderDate)
 * - Status filtering (status + orderDate)
 * - Date range queries (orderDate)
 * - Price range queries (total)
 * - Combined customer + status queries (customerId + status + orderDate)
 * 
 * MongoDB Best Practices:
 * - Indexes are created at application startup
 * - Compound indexes support multiple query patterns
 * - Sort order matches query patterns (DESC for dates)
 * - Existing indexes are not recreated (idempotent)
 */
@Configuration
@RequiredArgsConstructor
public class OrderIndexConfiguration {

    private final MongoTemplate mongoTemplate;

    /**
     * Create indexes after application context is initialized.
     * This runs once at startup.
     */
    @PostConstruct
    public void initIndexes() {
        createOrderIndexes();
    }

    /**
     * Create all required indexes for the orders collection.
     * 
     * Performance Impact:
     * - Without indexes: O(n) table scans (slow with millions of orders)
     * - With indexes: O(log n) index lookups (fast even with millions of orders)
     */
    private void createOrderIndexes() {
        String collectionName = "orders";

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 1: Customer orders sorted by date (newest first)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /customers/{id}/orders
        //           GET /orders?customerId=xxx
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("customerId", Sort.Direction.ASC)
                .on("orderDate", Sort.Direction.DESC)
                .named("idx_customerId_orderDate"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 2: Status filtering sorted by date (newest first)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /orders?status=PENDING
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("orderDate", Sort.Direction.DESC)
                .named("idx_status_orderDate"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 3: Date range queries
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /orders/search/by-date?startDate=xxx&endDate=yyy
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("orderDate", Sort.Direction.DESC)
                .named("idx_orderDate"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 4: Price range queries
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /orders/search/by-total?minTotal=xxx&maxTotal=yyy
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("total", Sort.Direction.ASC)
                .named("idx_total"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 5: Customer + Status compound query
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /orders/search/by-customer?customerId=xxx&status=PENDING
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("customerId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("orderDate", Sort.Direction.DESC)
                .named("idx_customerId_status_orderDate"));

        System.out.println("✅ Order indexes created successfully!");
        System.out.println("   - idx_customerId_orderDate");
        System.out.println("   - idx_status_orderDate");
        System.out.println("   - idx_orderDate");
        System.out.println("   - idx_total");
        System.out.println("   - idx_customerId_status_orderDate");
    }
}

