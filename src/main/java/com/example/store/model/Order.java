package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order entity - represents a customer order in MongoDB.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This class shows MongoDB's key principle:
 *    "Data that is accessed together should be stored together."
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: DOCUMENT VERSIONING PATTERN (Schema Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The Document Versioning Pattern tracks schema changes over time.
 * This allows you to evolve your schema while maintaining backward compatibility.
 *
 * Schema Evolution History:
 *
 * Version 1 (Original):
 * {
 *   "_id": "order123",
 *   "customer": "John Doe",           ← Simple string (v1)
 *   "orderDate": "2024-01-15",
 *   "items": [...],
 *   "total": 1299.99
 * }
 *
 * Version 2 (Subset Pattern):
 * {
 *   "_id": "order456",
 *   "schemaVersion": 2,                ← Version tracking
 *   "customerId": "cust789",           ← Reference added (v2)
 *   "customerName": "Jane Smith",      ← Subset pattern (v2)
 *   "orderDate": "2024-02-20",
 *   "items": [...],
 *   "total": 899.99
 * }
 *
 * Version 3 (Current - Outlier Pattern):
 * {
 *   "_id": "order789",
 *   "schemaVersion": 3,                ← Version tracking
 *   "customerId": "cust123",
 *   "customerName": "Alice Johnson",
 *   "orderDate": "2024-03-10",
 *   "items": [...],                    ← Embedded for normal orders (< 50 items)
 *   "isLargeOrder": false,             ← Outlier Pattern flag (v3)
 *   "total": 1299.99
 * }
 *
 * Version 3 (Large Order - 100+ items):
 * {
 *   "_id": "order999",
 *   "schemaVersion": 3,
 *   "customerId": "cust456",
 *   "customerName": "Bob Smith",
 *   "orderDate": "2024-03-10",
 *   "items": null,                     ← No embedded items for large orders
 *   "isLargeOrder": true,              ← Outlier Pattern flag (v3)
 *   "totalItemCount": 150,             ← Total items (v3)
 *   "bucketCount": 3,                  ← Number of buckets (v3)
 *   "total": 15000.00
 * }
 *
 * Benefits:
 * - Track which schema version each document uses
 * - Migrate documents gradually (not all at once)
 * - Application can handle both old and new formats
 * - Safe schema evolution in production
 *
 * Migration Strategy:
 * - New orders created with schemaVersion = 2
 * - Old orders (v1) can be migrated on-demand or in batches
 * - Application code can check version and handle accordingly
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: OUTLIER PATTERN (Schema Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The Outlier Pattern handles cases where most documents are normal-sized,
 * but a few are exceptionally large (outliers).
 *
 * The Problem:
 * - MongoDB has a 16MB document size limit
 * - Most orders have 1-20 items (normal case)
 * - Some orders have 50-100+ items (outliers)
 * - Large embedded arrays hurt performance
 *
 * The Solution:
 * - Embed items for normal orders (< 50 items) → Fast, simple
 * - Flag large orders with isLargeOrder = true
 * - For 100+ items: Split into buckets (separate documents)
 * - For 500+ items: Store each item separately
 *
 * Decision Framework:
 * - 1-50 items:    Embed in Order (99% of cases)
 * - 50-100 items:  Embed + flag as large (0.9% of cases)
 * - 100-500 items: Use bucketing (0.09% of cases)
 * - 500+ items:    Separate collection (0.01% of cases)
 *
 * Example Large Order (100+ items):
 * Main Order Document:
 * {
 *   "_id": "order999",
 *   "isLargeOrder": true,
 *   "totalItemCount": 150,
 *   "bucketCount": 3,
 *   "items": null  // Items stored in separate bucket documents
 * }
 *
 * Bucket Documents (in "order_item_buckets" collection):
 * {
 *   "_id": "order999_bucket_0",
 *   "orderId": "order999",
 *   "bucketNumber": 0,
 *   "items": [ ... 50 items ... ]
 * }
 * {
 *   "_id": "order999_bucket_1",
 *   "orderId": "order999",
 *   "bucketNumber": 1,
 *   "items": [ ... 50 items ... ]
 * }
 * {
 *   "_id": "order999_bucket_2",
 *   "orderId": "order999",
 *   "bucketNumber": 2,
 *   "items": [ ... 50 items ... ]
 * }
 *
 * Benefits:
 * - Optimize for the common case (small orders)
 * - Handle outliers gracefully (large orders)
 * - Stay well below 16MB document limit
 * - Maintain good query performance
 *
 * ───────────────────────────────────────────────────────────────────────────
 *
 * When you view an order, you need:
 * ✅ Customer name (to show who ordered)
 * ✅ Order items (to show what was ordered)
 * ✅ Order date and total
 *
 * You DON'T usually need:
 * ❌ Customer's email or phone
 * ❌ Current product inventory
 *
 * So we store what we need together!
 *
 * ───────────────────────────────────────────────────────────────────────────
 * THREE PATTERNS USED IN THIS CLASS:
 * ───────────────────────────────────────────────────────────────────────────
 *
 * 1. 📦 EMBEDDING (items field)
 *    Question: Do I ALWAYS need order items when viewing an order?
 *    Answer: YES! ✅
 *    Solution: Store items INSIDE the order (embedded)
 *    Benefit: One query gets everything - no joins needed!
 *
 * 2. 🔗 SUBSET PATTERN (customerId + customerName)
 *    Question: Do I FREQUENTLY need customer name?
 *    Answer: YES! ✅
 *    Question: Do I FREQUENTLY need customer email/phone?
 *    Answer: NO! ❌
 *    Solution: Store customer name (for speed) + customerId (for full data)
 *    Benefit: Fast queries 90% of the time, full data available when needed
 *
 * 3. 🔗 REFERENCE (customerId)
 *    Question: Do I OCCASIONALLY need full customer data?
 *    Answer: YES, but rarely
 *    Solution: Store link (customerId) to full customer document
 *    Benefit: Can get full customer profile when needed
 *
 * ───────────────────────────────────────────────────────────────────────────
 * SIMPLE RULE:
 * - ALWAYS need it? → Store it together (embed)
 * - FREQUENTLY need it? → Copy it + keep a link (subset)
 * - RARELY need it? → Just keep a link (reference)
 * ───────────────────────────────────────────────────────────────────────────
 */
@Data                                    // Lombok: generates getters/setters
@NoArgsConstructor                       // Lombok: generates no-args constructor
@AllArgsConstructor                      // Lombok: generates all-args constructor
@Document(collection = "orders")         // MongoDB: stores in "orders" collection
public class Order {

    @Id                                  // MongoDB: this is the document ID (_id)
    private String id;

    // 📋 DOCUMENT VERSIONING PATTERN: Track schema evolution
    private Integer schemaVersion = 3;   // Current version (v1 = customer string, v2 = customerId + customerName, v3 = added Outlier Pattern)

    // 🔗 SUBSET PATTERN: We store BOTH a link and a copy
    private String customerId;           // Link to full Customer (for email, phone, etc.)
    private String customerName;         // Copy of name (for fast display)

    private LocalDateTime orderDate;     // When the order was placed

    // 📦 EMBEDDING PATTERN: Items stored INSIDE the order (for normal orders)
    private List<OrderItem> items;       // All order items in one place!

    // 🎯 OUTLIER PATTERN: Handle orders with many items (50+)
    private Boolean isLargeOrder = false;  // Flag: true if order has 50+ items
    private Integer totalItemCount;        // Total number of items (for large orders)
    private Integer bucketCount;           // Number of buckets (for orders with 100+ items)

    // 🧮 COMPUTED PATTERN: Pre-calculated value stored in document
    private BigDecimal total;            // Total price (calculated once, stored for fast reads)
}

