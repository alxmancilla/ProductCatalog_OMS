package com.example.store.config;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Seeds legacy schema-version documents on first startup.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Make the DOCUMENT VERSIONING PATTERN tangible in the demo
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * After seeding you can run these in mongosh / Compass to see all four
 * product schema versions and all four order schema versions living side-by-side:
 *
 *   db.products.find({}, {name:1, schemaVersion:1}).sort({schemaVersion:1})
 *   db.orders.find({}, {customerName:1, schemaVersion:1, status:1}).sort({schemaVersion:1})
 *
 * Key teaching points:
 * - v1 documents have NO schemaVersion field (field is absent / null)
 * - v2 documents added inventory + sku + description
 * - Orders evolved from a plain customerName string → customerId reference
 *   → Outlier Pattern flag → full Status Management with audit trail
 * - The application handles all versions transparently
 *
 * This seeder runs ONCE: it checks for a sentinel document in a
 * "demo_meta" collection before inserting, so restarts are safe.
 */
@Component
@Order(2)   // Run after MongoSchemaValidation (which is @Order(1) by default)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String META_COLLECTION = "demo_meta";
    private static final String SEED_KEY = "legacy_schema_versions_seeded";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        if (alreadySeeded()) {
            logger.info("📋 Demo seed data already present — skipping.");
            return;
        }

        logger.info("🌱 Seeding legacy schema-version documents for Document Versioning demo...");

        seedLegacyProducts();
        seedLegacyOrders();
        markSeeded();

        logger.info("✅ Legacy seed documents inserted.");
        logger.info("   Run in mongosh:  db.products.find({},{name:1,schemaVersion:1}).sort({schemaVersion:1})");
        logger.info("   Run in mongosh:  db.orders.find({},{customerName:1,schemaVersion:1}).sort({schemaVersion:1})");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRODUCT SCHEMA VERSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private void seedLegacyProducts() {
        // ── Version 1 ──────────────────────────────────────────────────────
        // Original schema: only name, price, category. No inventory, no sku,
        // no description, no schemaVersion field.
        // This is what the products collection looked like at launch.
        Document productV1 = new Document()
            .append("_id", "legacy-prod-v1-001")
            .append("name", "Laptop Pro 15 [v1 Legacy]")
            .append("price", 1299.99)
            .append("category", "Electronics");
        // Note: no schemaVersion, no inventory, no sku, no description

        // ── Version 2 ──────────────────────────────────────────────────────
        // Added inventory tracking, SKU, and description.
        // Products now support the AI search features.
        Document productV2 = new Document()
            .append("_id", "legacy-prod-v2-001")
            .append("schemaVersion", 2)
            .append("name", "Laptop Pro 15 [v2 Legacy]")
            .append("description", "High-performance laptop for professionals")
            .append("price", 1299.99)
            .append("category", "Electronics")
            .append("inventory", 50)
            .append("sku", "LAPTOP-V2-001");

        // ── Version 2 with Polymorphic Details (current) ───────────────────
        // Type-specific fields moved into nested detail objects.
        // This is the current default for new products.
        // (Already created via the API — this is just for completeness.)

        insertIfAbsent("products", productV1, "legacy-prod-v1-001");
        insertIfAbsent("products", productV2, "legacy-prod-v2-001");

        logger.info("   Inserted product v1 (no schemaVersion field) and v2 (schemaVersion: 2)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDER SCHEMA VERSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private void seedLegacyOrders() {
        Date now = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));

        // ── Version 1 ──────────────────────────────────────────────────────
        // Customer stored as a plain string — no reference, no ID.
        // Simple embedding of items.
        Document orderV1 = new Document()
            .append("_id", "legacy-order-v1-001")
            .append("customer", "John Doe")                // v1: plain string, no customerId
            .append("orderDate", now)
            .append("items", List.of(
                new Document("name", "Laptop Pro 15").append("price", 1299.99).append("quantity", 1)
            ))
            .append("total", 1299.99);
        // Note: no schemaVersion, no customerId, customer is a string

        // ── Version 2 ──────────────────────────────────────────────────────
        // Introduced the Subset Pattern: store customerId (reference) AND
        // customerName (denormalized copy) for fast display.
        Document orderV2 = new Document()
            .append("_id", "legacy-order-v2-001")
            .append("schemaVersion", 2)
            .append("customerId", "cust-legacy-001")       // v2: reference added
            .append("customerName", "Jane Smith")           // v2: Subset Pattern — copy for fast reads
            .append("orderDate", now)
            .append("items", List.of(
                new Document("productId", "prod-001")
                    .append("name", "Laptop Pro 15")
                    .append("price", 1299.99)
                    .append("quantity", 1)
            ))
            .append("total", 1299.99);

        // ── Version 3 ──────────────────────────────────────────────────────
        // Added the Outlier Pattern: isLargeOrder flag to handle orders
        // with 50+ items without hitting MongoDB's 16 MB document limit.
        Document orderV3 = new Document()
            .append("_id", "legacy-order-v3-001")
            .append("schemaVersion", 3)
            .append("customerId", "cust-legacy-002")
            .append("customerName", "Alice Johnson")
            .append("orderDate", now)
            .append("items", List.of(
                new Document("productId", "prod-001")
                    .append("name", "Laptop Pro 15")
                    .append("price", 1299.99)
                    .append("quantity", 2)
            ))
            .append("isLargeOrder", false)                 // v3: Outlier Pattern flag added
            .append("total", 2599.98);

        // ── Version 4 (current) ────────────────────────────────────────────
        // Added full Order Status Management: status field + statusHistory
        // array for a complete, embedded audit trail.
        Document orderV4 = new Document()
            .append("_id", "legacy-order-v4-001")
            .append("schemaVersion", 4)
            .append("customerId", "cust-legacy-003")
            .append("customerName", "Bob Smith")
            .append("orderDate", now)
            .append("status", "PENDING")                   // v4: current status (denormalized)
            .append("statusHistory", List.of(              // v4: embedded audit trail
                new Document("fromStatus", null)
                    .append("toStatus", "PENDING")
                    .append("changedAt", now)
                    .append("changedBy", "system")
                    .append("reason", "Order created")
            ))
            .append("items", List.of(
                new Document("productId", "prod-001")
                    .append("name", "Laptop Pro 15")
                    .append("price", 1299.99)
                    .append("quantity", 1)
            ))
            .append("isLargeOrder", false)
            .append("total", 1299.99);

        insertIfAbsent("orders", orderV1, "legacy-order-v1-001");
        insertIfAbsent("orders", orderV2, "legacy-order-v2-001");
        insertIfAbsent("orders", orderV3, "legacy-order-v3-001");
        insertIfAbsent("orders", orderV4, "legacy-order-v4-001");

        logger.info("   Inserted order v1 (customer string), v2 (Subset Pattern),");
        logger.info("   v3 (Outlier Pattern flag), and v4 (Status Management)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void insertIfAbsent(String collection, Document doc, String id) {
        if (mongoTemplate.getCollection(collection).countDocuments(new Document("_id", id)) == 0) {
            mongoTemplate.getCollection(collection).insertOne(doc);
        }
    }

    private boolean alreadySeeded() {
        return mongoTemplate.getCollection(META_COLLECTION)
            .countDocuments(new Document("_id", SEED_KEY)) > 0;
    }

    private void markSeeded() {
        mongoTemplate.getCollection(META_COLLECTION)
            .insertOne(new Document("_id", SEED_KEY).append("seededAt", new java.util.Date()));
    }
}
