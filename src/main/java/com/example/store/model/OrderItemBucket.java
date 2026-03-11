package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * OrderItemBucket - stores batches of order items for large orders.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This is the OUTLIER PATTERN in action!
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Why does this exist?
 * - Most orders have 1-20 items → Embed in Order document (fast!)
 * - Some orders have 100+ items → Too large to embed efficiently
 * - Solution: Split large orders into "buckets" of 50 items each
 * 
 * How it works:
 * 
 * Normal Order (20 items):
 * {
 *   "_id": "order123",
 *   "items": [ ... 20 items embedded ... ],
 *   "isLargeOrder": false
 * }
 * 
 * Large Order (150 items):
 * Main Order Document:
 * {
 *   "_id": "order456",
 *   "items": null,              ← No embedded items
 *   "isLargeOrder": true,
 *   "totalItemCount": 150,
 *   "bucketCount": 3            ← 3 buckets of 50 items each
 * }
 * 
 * Bucket 0 (items 1-50):
 * {
 *   "_id": "order456_bucket_0",
 *   "orderId": "order456",
 *   "bucketNumber": 0,
 *   "items": [ ... 50 items ... ]
 * }
 * 
 * Bucket 1 (items 51-100):
 * {
 *   "_id": "order456_bucket_1",
 *   "orderId": "order456",
 *   "bucketNumber": 1,
 *   "items": [ ... 50 items ... ]
 * }
 * 
 * Bucket 2 (items 101-150):
 * {
 *   "_id": "order456_bucket_2",
 *   "orderId": "order456",
 *   "bucketNumber": 2,
 *   "items": [ ... 50 items ... ]
 * }
 * 
 * Benefits:
 * - ✅ Handles orders with 100s or 1000s of items
 * - ✅ Stays well below MongoDB's 16MB document limit
 * - ✅ Can paginate through items efficiently
 * - ✅ Optimizes for the common case (small orders)
 * 
 * Query Example:
 * // Get all buckets for an order
 * db.order_item_buckets.find({ "orderId": "order456" }).sort({ "bucketNumber": 1 })
 * 
 * // Get specific bucket (pagination)
 * db.order_item_buckets.findOne({ "orderId": "order456", "bucketNumber": 0 })
 * 
 * ───────────────────────────────────────────────────────────────────────────
 * 
 * This is a SEPARATE collection (not embedded) because:
 * - Large orders are rare (outliers)
 * - We don't want to slow down normal orders
 * - Bucketing allows efficient pagination
 */
@Data                                           // Lombok: generates getters/setters
@NoArgsConstructor                              // Lombok: generates no-args constructor
@AllArgsConstructor                             // Lombok: generates all-args constructor
@Document(collection = "order_item_buckets")    // MongoDB: stores in "order_item_buckets" collection
public class OrderItemBucket {
    
    @Id                                         // MongoDB: this is the document ID (_id)
    private String id;                          // Format: "{orderId}_bucket_{bucketNumber}"
    
    private String orderId;                     // Reference to the parent Order
    
    private Integer bucketNumber;               // Bucket index (0, 1, 2, ...)
    
    private List<OrderItem> items;              // Batch of items (typically 50 per bucket)
}

