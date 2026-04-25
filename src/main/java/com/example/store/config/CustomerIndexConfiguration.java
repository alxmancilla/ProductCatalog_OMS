package com.example.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import jakarta.annotation.PostConstruct;

/**
 * Configuration class for creating MongoDB indexes on the customers collection.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Performance Optimization for Customer Queries (P1 FIX)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This class creates indexes to support fast queries for:
 * - Customer lookup by email (common authentication use case)
 * - Customer filtering by tier (PLATINUM, GOLD, STANDARD)
 * - Combined tier + name queries for customer lists
 * 
 * MongoDB Best Practices:
 * - Indexes are created at application startup
 * - Unique constraint on email prevents duplicates
 * - Existing indexes are not recreated (idempotent)
 */
@Configuration
@RequiredArgsConstructor
public class CustomerIndexConfiguration {

    private final MongoTemplate mongoTemplate;

    /**
     * Create indexes after application context is initialized.
     * This runs once at startup.
     */
    @PostConstruct
    public void initIndexes() {
        createCustomerIndexes();
    }

    /**
     * Create all required indexes for the customers collection.
     * 
     * P1 Fix: Add email index for authentication/lookup performance
     * 
     * Performance Impact:
     * - Without email index: O(n) table scan for login
     * - With email index: O(1) hash lookup or O(log n) B-tree
     */
    private void createCustomerIndexes() {
        String collectionName = "customers";

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 1: Email - Unique identifier (P1 FIX)
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByEmail()
        //           Customer authentication
        //           Email uniqueness validation
        // 
        // Why unique:
        // - Email is the primary way to identify customers
        // - Prevents duplicate accounts
        // - Critical for authentication performance
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("email", Sort.Direction.ASC)
                .unique()
                .named("idx_email_unique"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 2: Customer tier for segmentation
        // ═══════════════════════════════════════════════════════════════════
        // Supports: findByTier()
        //           GET /customers/search/by-tier?tier=PLATINUM
        //           Customer segmentation queries
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("tier", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .named("idx_tier_name"));

        // ═══════════════════════════════════════════════════════════════════
        // INDEX 3: Name for alphabetical sorting
        // ═══════════════════════════════════════════════════════════════════
        // Supports: GET /customers (with sort by name)
        //           Customer search/autocomplete
        mongoTemplate.indexOps(collectionName)
            .ensureIndex(new Index()
                .on("name", Sort.Direction.ASC)
                .named("idx_name"));

        System.out.println("✅ Customer indexes created successfully!");
        System.out.println("   - idx_email_unique (P1 FIX)");
        System.out.println("   - idx_tier_name");
        System.out.println("   - idx_name");
    }
}
