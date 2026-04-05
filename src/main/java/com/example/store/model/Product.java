package com.example.store.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product entity - represents a product in the catalog.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This is a SEPARATE collection (not embedded)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This class becomes a "products" collection in MongoDB.
 * Each product is stored as a separate document.
 *
 * Why separate?
 * - Products are shared across many orders
 * - Product data (price, inventory) changes frequently
 * - We want ONE place to update product information
 *
 * How do Orders connect to Products?
 * - OrderItems store the product's ID (productId)
 * - OrderItems also copy the product's name and price (historical snapshot)
 * - This is the "Subset Pattern" - copy what you need at order time!
 *
 * Why copy price?
 * - If a product's price changes, old orders should show the OLD price
 * - This preserves historical accuracy
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
 * Version 1 (Original - Basic Product):
 * {
 *   "_id": "prod123",
 *   "name": "Laptop Pro 15",
 *   "price": 1299.99,
 *   "category": "Electronics"
 * }
 *
 * Version 2 (Added Inventory & SKU):
 * {
 *   "_id": "prod456",
 *   "schemaVersion": 2,                ← Version tracking
 *   "name": "Laptop Pro 15",
 *   "description": "High-performance laptop",
 *   "price": 1299.99,
 *   "category": "Electronics",
 *   "inventory": 50,                   ← Added in v2
 *   "sku": "LAPTOP-001"                ← Added in v2
 * }
 *
 * Benefits:
 * - Track which schema version each document uses
 * - Migrate documents gradually (not all at once)
 * - Application can handle both old and new formats
 * - Safe schema evolution in production
 *
 * Migration Strategy:
 * - New products created with schemaVersion = 2
 * - Old products (v1) can be migrated on-demand or in batches
 * - Application code can check version and handle accordingly
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: POLYMORPHIC PATTERN (Schema Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The Polymorphic Pattern allows documents in the same collection to have
 * different fields based on their type. This shows MongoDB's schema flexibility!
 *
 * Example - Electronics product:
 * {
 *   "_id": "prod123",
 *   "schemaVersion": 2,
 *   "type": "Electronics",
 *   "name": "Laptop Pro 15",                                    ← REQUIRED
 *   "description": "High-performance laptop for professionals", ← REQUIRED
 *   "price": 1299.99,                                           ← REQUIRED
 *   "category": "Electronics",                                  ← REQUIRED
 *   "inventory": 50,                                            ← REQUIRED
 *   "sku": "LAPTOP-001",                                        ← REQUIRED
 *   "warranty": "2 years",        ← Electronics-specific field
 *   "brand": "TechCorp"            ← Electronics-specific field
 * }
 *
 * Example - Clothing product:
 * {
 *   "_id": "prod456",
 *   "schemaVersion": 2,
 *   "type": "Clothing",
 *   "name": "Cotton T-Shirt",                                   ← REQUIRED
 *   "description": "Premium quality cotton t-shirt",            ← REQUIRED
 *   "price": 29.99,                                             ← REQUIRED
 *   "category": "Clothing",                                     ← REQUIRED
 *   "inventory": 200,                                           ← REQUIRED
 *   "sku": "TSHIRT-001",                                        ← REQUIRED
 *   "size": "L",                   ← Clothing-specific field
 *   "color": "Blue",               ← Clothing-specific field
 *   "material": "100% Cotton"      ← Clothing-specific field
 * }
 *
 * Example - Book product:
 * {
 *   "_id": "prod789",
 *   "schemaVersion": 2,
 *   "type": "Book",
 *   "name": "MongoDB Guide",                                    ← REQUIRED
 *   "description": "Comprehensive guide to MongoDB patterns",   ← REQUIRED
 *   "price": 49.99,                                             ← REQUIRED
 *   "category": "Books",                                        ← REQUIRED
 *   "inventory": 100,                                           ← REQUIRED
 *   "sku": "BOOK-001",                                          ← REQUIRED
 *   "author": "Jane Smith",        ← Book-specific field
 *   "isbn": "978-1234567890",      ← Book-specific field
 *   "pages": 350                   ← Book-specific field
 * }
 *
 * Benefits:
 * - All products in ONE collection (simpler queries)
 * - Each type can have unique fields (flexibility)
 * - No need for separate tables/collections per product type
 * - Easy to add new product types without schema changes
 */
@Data                                    // Lombok: generates getters/setters
@NoArgsConstructor                       // Lombok: generates no-args constructor
@AllArgsConstructor                      // Lombok: generates all-args constructor
@Document(collection = "products")       // MongoDB: stores in "products" collection
public class Product {

    @Id                                  // MongoDB: this is the document ID (_id)
    private String id;

    // 📋 DOCUMENT VERSIONING PATTERN: Track schema evolution
    private Integer schemaVersion = 2;   // Current version (v1 = basic fields, v2 = added inventory + sku + description)

    // ═══════════════════════════════════════════════════════════════════════
    // COMMON FIELDS (all product types have these)
    // ═══════════════════════════════════════════════════════════════════════

    private String type;                 // 🎯 POLYMORPHIC: "Electronics", "Clothing", "Book", or "General"

    @NotBlank(message = "Product name is required")
    private String name;                 // Product name (REQUIRED)

    @NotBlank(message = "Product description is required")
    private String description;          // 🆕 Detailed product description (REQUIRED - for AI matching & display)

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than zero")
    private BigDecimal price;            // Current price (REQUIRED - can change over time)

    @NotBlank(message = "Product category is required")
    private String category;             // Product category (REQUIRED - e.g., "Electronics")

    @NotNull(message = "Product inventory is required")
    @PositiveOrZero(message = "Product inventory must be zero or greater")
    private Integer inventory;           // How many are in stock (REQUIRED)

    @NotBlank(message = "Product SKU is required")
    private String sku;                  // Stock Keeping Unit (unique product identifier) (REQUIRED)

    // ═══════════════════════════════════════════════════════════════════════
    // AI/SEARCH FIELDS (for MongoDB Hybrid Search)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 🤖 VECTOR EMBEDDING: Numerical representation of product description
     *
     * This field stores the vector embedding generated by Voyage AI (voyage-4-large model).
     * Used for semantic search in MongoDB Atlas Vector Search.
     *
     * - Generated from the product's description field
     * - Dimension: 1024 (voyage-4-large output size)
     * - Used in $vectorSearch aggregation for hybrid search
     * - Enables semantic matching: "running shoes" matches "athletic sneakers"
     *
     * Example:
     * descriptionEmbedding: [0.234, -0.567, 0.891, ..., 0.123] (1024 dimensions)
     */
    private List<Double> descriptionEmbedding;  // Vector embedding for semantic search (1024 dimensions)

    // ═══════════════════════════════════════════════════════════════════════
    // POLYMORPHIC FIELDS (using Composition Pattern - embedded detail objects)
    // ═══════════════════════════════════════════════════════════════════════
    // MongoDB allows flexible schemas - we use embedded objects to group type-specific fields!

    /**
     * 🏭 COMPOSITION PATTERN: Electronics-specific details
     *
     * This field is ONLY populated for Electronics products.
     * Contains warranty, brand, weight, screenSize, resolution, capacity.
     *
     * Example in MongoDB:
     * "electronicsDetails": {
     *   "warranty": "2 years",
     *   "brand": "TechCorp",
     *   "weight": "1.8 kg",
     *   "screenSize": "15.6 inch"
     * }
     *
     * Benefits:
     * - Clear grouping of electronics-specific fields
     * - Easy to validate as a unit
     * - null for non-electronics products
     */
    private ElectronicsDetails electronicsDetails;

    /**
     * 👕 COMPOSITION PATTERN: Clothing-specific details
     *
     * This field is ONLY populated for Clothing products.
     * Contains size, color, material.
     *
     * Example in MongoDB:
     * "clothingDetails": {
     *   "size": "L",
     *   "color": "Blue",
     *   "material": "100% Cotton"
     * }
     */
    private ClothingDetails clothingDetails;

    /**
     * 📚 COMPOSITION PATTERN: Book-specific details
     *
     * This field is ONLY populated for Book products.
     * Contains author, isbn, pages, publisher, language.
     *
     * Example in MongoDB:
     * "bookDetails": {
     *   "author": "Jane Smith",
     *   "isbn": "978-1234567890",
     *   "pages": 350,
     *   "publisher": "TechBooks Publishing",
     *   "language": "English"
     * }
     */
    private BookDetails bookDetails;

}

