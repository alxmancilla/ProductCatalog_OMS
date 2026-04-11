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
 * Legacy documents are written to SEPARATE unvalidated collections
 * (products_legacy, orders_legacy) so they don't conflict with the JSON
 * Schema validation rules on the live collections — and so the demo user
 * doesn't need elevated Atlas privileges (bypassDocumentValidation requires
 * dbAdmin or higher).
 *
 * After startup, run these in mongosh / Compass to see all schema versions:
 *
 *   // Products: v1 (no schemaVersion) → v2 (added inventory/sku/description)
 *   db.products_legacy.find({}, {name:1, schemaVersion:1}).sort({schemaVersion:1})
 *
 *   // Orders: v1 (customer string) → v2 (Subset) → v3 (Outlier) → v4 (Status)
 *   db.orders_legacy.find({}, {schemaVersion:1}).sort({schemaVersion:1})
 *
 * Key teaching points:
 * - v1 documents have NO schemaVersion field (field is absent / null)
 * - The shape of the document changed significantly across versions
 * - Current live documents (in products / orders) are always v2 / v4
 * - Schema validation enforces the CURRENT schema — legacy docs would be
 *   rejected if inserted into the live collection (that's by design!)
 *
 * This seeder runs ONCE: idempotent via a sentinel in the demo_meta collection.
 */
@Component
@Order(2)   // Run after MongoSchemaValidation (which is @Order(1) by default)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String META_COLLECTION  = "demo_meta";
    private static final String SEED_KEY         = "legacy_schema_versions_seeded_v2";

    // Separate collections — no JSON Schema validation applied, no elevated privileges needed
    private static final String PRODUCTS_LEGACY = "products_legacy";
    private static final String ORDERS_LEGACY   = "orders_legacy";

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
        logger.info("   Products (v1→v2):  db.products_legacy.find({},{name:1,schemaVersion:1}).sort({schemaVersion:1})");
        logger.info("   Orders  (v1→v4):   db.orders_legacy.find({},{schemaVersion:1}).sort({schemaVersion:1})");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRODUCT SCHEMA VERSIONS  (written to products_legacy)
    // ═══════════════════════════════════════════════════════════════════════

    private void seedLegacyProducts() {
        // ── Version 1 ──────────────────────────────────────────────────────
        // Original schema: only name, price, category.
        // No inventory, no sku, no description, no schemaVersion field.
        // Teaching point: this would FAIL the current JSON Schema validation
        // on the live products collection — that's the evolution story!
        Document productV1 = new Document()
            .append("_id", "legacy-prod-v1-001")
            .append("name", "Laptop Pro 15")
            .append("price", 1299.99)
            .append("category", "Electronics");
        // Intentionally missing: schemaVersion, inventory, sku, description

        // ── Version 2 ──────────────────────────────────────────────────────
        // Added inventory tracking, SKU, and description.
        // This matches what the current products collection requires.
        Document productV2 = new Document()
            .append("_id", "legacy-prod-v2-001")
            .append("schemaVersion", 2)
            .append("name", "Laptop Pro 15")
            .append("description", "High-performance laptop for professionals")
            .append("price", 1299.99)
            .append("category", "Electronics")
            .append("inventory", 50)
            .append("sku", "LAPTOP-V2-001");

        // ── Version 2 with Polymorphic Details (current shape) ─────────────
        // Type-specific fields now grouped into nested detail objects.
        // New products look like this in the live collection.
        Document productV2Polymorphic = new Document()
            .append("_id", "legacy-prod-v2-poly-001")
            .append("schemaVersion", 2)
            .append("type", "Electronics")
            .append("name", "Laptop Pro 15")
            .append("description", "High-performance laptop for professionals")
            .append("price", 1299.99)
            .append("category", "Electronics")
            .append("inventory", 50)
            .append("sku", "LAPTOP-POLY-001")
            .append("electronicsDetails", new Document()
                .append("warranty", "2 years")
                .append("brand", "TechCorp")
                .append("weight", "1.8 kg")
                .append("screenSize", "15.6 inch"));

        insertIfAbsent(PRODUCTS_LEGACY, productV1,          "legacy-prod-v1-001");
        insertIfAbsent(PRODUCTS_LEGACY, productV2,          "legacy-prod-v2-001");
        insertIfAbsent(PRODUCTS_LEGACY, productV2Polymorphic, "legacy-prod-v2-poly-001");

        logger.info("   products_legacy: v1 (bare schema), v2 (+ inventory/sku), v2-poly (+ nested details)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDER SCHEMA VERSIONS  (written to orders_legacy)
    // ═══════════════════════════════════════════════════════════════════════

    private void seedLegacyOrders() {
        Date now = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));

        // ── Version 1 ──────────────────────────────────────────────────────
        // Customer stored as a plain string — no reference, no ID.
        // Teaching point: changing price in the products collection wouldn't
        // affect old orders, but there's no way to look up the customer record.
        Document orderV1 = new Document()
            .append("_id", "legacy-order-v1-001")
            .append("customer", "John Doe")            // v1: plain string (no customerId)
            .append("orderDate", now)
            .append("items", List.of(
                new Document("name", "Laptop Pro 15")
                    .append("price", 1299.99)
                    .append("quantity", 1)
            ))
            .append("total", 1299.99);
        // No schemaVersion, no customerId, customer is a bare string

        // ── Version 2 ──────────────────────────────────────────────────────
        // Subset Pattern introduced: customerId (reference) + customerName
        // (denormalized copy). Fast reads without a join, full data via ID.
        Document orderV2 = new Document()
            .append("_id", "legacy-order-v2-001")
            .append("schemaVersion", 2)
            .append("customerId", "cust-legacy-001")   // v2: reference to Customer doc
            .append("customerName", "Jane Smith")       // v2: Subset Pattern — copy for fast reads
            .append("orderDate", now)
            .append("items", List.of(
                new Document("productId", "prod-001")
                    .append("name", "Laptop Pro 15")
                    .append("price", 1299.99)
                    .append("quantity", 1)
            ))
            .append("total", 1299.99);

        // ── Version 3 ──────────────────────────────────────────────────────
        // Outlier Pattern introduced: isLargeOrder flag.
        // Orders with 50+ items can now be flagged; 100+ items use buckets.
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
            .append("isLargeOrder", false)             // v3: Outlier Pattern flag
            .append("total", 2599.98);

        // ── Version 4 (current) ────────────────────────────────────────────
        // Status Management added: status field (denormalized) + statusHistory
        // array (embedded audit trail). Single-document update = atomic.
        Document orderV4 = new Document()
            .append("_id", "legacy-order-v4-001")
            .append("schemaVersion", 4)
            .append("customerId", "cust-legacy-003")
            .append("customerName", "Bob Smith")
            .append("orderDate", now)
            .append("status", "PENDING")               // v4: denormalized current status
            .append("statusHistory", List.of(          // v4: embedded audit trail
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

        insertIfAbsent(ORDERS_LEGACY, orderV1, "legacy-order-v1-001");
        insertIfAbsent(ORDERS_LEGACY, orderV2, "legacy-order-v2-001");
        insertIfAbsent(ORDERS_LEGACY, orderV3, "legacy-order-v3-001");
        insertIfAbsent(ORDERS_LEGACY, orderV4, "legacy-order-v4-001");

        logger.info("   orders_legacy: v1 (customer string), v2 (Subset), v3 (Outlier flag), v4 (Status)");
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
