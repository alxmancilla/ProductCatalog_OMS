package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Inventory reservation — a temporary hold placed on product stock during checkout.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: MongoDB TTL (Time-To-Live) Index
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * A TTL index automatically deletes documents once the value of the indexed
 * date field passes. This is a native MongoDB feature with no SQL equivalent —
 * you cannot do this in a relational database without a background job.
 *
 * How it works:
 *   1. Customer requests a hold on N units of a product.
 *   2. A reservation document is created with expiresAt = now + holdMinutes.
 *   3. MongoDB's TTL monitor (runs every ~60 s) deletes expired documents.
 *   4. When deleted, the hold is automatically freed — no cleanup code needed.
 *
 * TTL Index definition (created automatically via @Indexed below):
 *   db.inventory_reservations.createIndex(
 *     { "expiresAt": 1 },
 *     { expireAfterSeconds: 0 }   ← delete when expiresAt is reached
 *   )
 *
 * Example document in MongoDB:
 * {
 *   "_id":          "res_abc123",
 *   "productId":    "prod_456",
 *   "productName":  "Laptop Pro 15",
 *   "quantity":     2,
 *   "customerId":   "cust_789",
 *   "customerName": "Alice Johnson",
 *   "reservedAt":   ISODate("2026-04-11T10:00:00Z"),
 *   "expiresAt":    ISODate("2026-04-11T10:15:00Z")  ← TTL field
 * }
 *
 * After 2026-04-11T10:15:00Z MongoDB silently removes this document.
 * The 2 units are then available again — zero application code involved.
 *
 * Available inventory formula:
 *   availableInventory = product.inventory
 *                      - SUM(activeReservations.quantity for this product)
 *
 * Note: In production you would also decrement product.inventory when placing
 * the real order and delete the reservation atomically in the same transaction.
 * This demo focuses on the TTL pattern itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory_reservations")
public class InventoryReservation {

    @Id
    private String id;

    private String productId;
    private String productName;
    private int quantity;

    private String customerId;    // optional — who holds this reservation
    private String customerName;  // optional

    private LocalDateTime reservedAt;

    /**
     * TTL field — MongoDB deletes this document automatically when
     * the current time passes this value.
     *
     * expireAfterSeconds = 0  means "delete at exactly the expiresAt timestamp."
     * Setting it to a positive N would mean "delete N seconds after expiresAt."
     *
     * IMPORTANT: spring.data.mongodb.auto-index-creation=true must be enabled
     * (see application.properties) for @Indexed to create the index on startup.
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;
}
