package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

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
 * 🎯 DEMONSTRATES: POLYMORPHIC PATTERN (Schema Design Pattern)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The Polymorphic Pattern allows documents in the same collection to have
 * different fields based on their type. This shows MongoDB's schema flexibility!
 *
 * Example - Electronics product:
 * {
 *   "_id": "prod123",
 *   "type": "Electronics",
 *   "name": "Laptop Pro 15",
 *   "price": 1299.99,
 *   "category": "Electronics",
 *   "inventory": 50,
 *   "warranty": "2 years",        ← Electronics-specific field
 *   "brand": "TechCorp"            ← Electronics-specific field
 * }
 *
 * Example - Clothing product:
 * {
 *   "_id": "prod456",
 *   "type": "Clothing",
 *   "name": "Cotton T-Shirt",
 *   "price": 29.99,
 *   "category": "Clothing",
 *   "inventory": 200,
 *   "size": "L",                   ← Clothing-specific field
 *   "color": "Blue",               ← Clothing-specific field
 *   "material": "100% Cotton"      ← Clothing-specific field
 * }
 *
 * Example - Book product:
 * {
 *   "_id": "prod789",
 *   "type": "Book",
 *   "name": "MongoDB Guide",
 *   "price": 49.99,
 *   "category": "Books",
 *   "inventory": 100,
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

    // ═══════════════════════════════════════════════════════════════════════
    // COMMON FIELDS (all product types have these)
    // ═══════════════════════════════════════════════════════════════════════

    private String type;                 // 🎯 POLYMORPHIC: "Electronics", "Clothing", "Book"

    private String name;                 // Product name

    private BigDecimal price;            // Current price (can change over time)

    private String category;             // Product category (e.g., "Electronics")

    private Integer inventory;           // How many are in stock

    // ═══════════════════════════════════════════════════════════════════════
    // POLYMORPHIC FIELDS (only some product types have these)
    // ═══════════════════════════════════════════════════════════════════════
    // MongoDB allows flexible schemas - not all products need all fields!

    // Electronics-specific fields
    private String warranty;             // e.g., "2 years" (only for Electronics)
    private String brand;                // e.g., "TechCorp" (only for Electronics)

    // Clothing-specific fields
    private String size;                 // e.g., "L", "XL" (only for Clothing)
    private String color;                // e.g., "Blue", "Red" (only for Clothing)
    private String material;             // e.g., "100% Cotton" (only for Clothing)

    // Book-specific fields
    private String author;               // e.g., "Jane Smith" (only for Books)
    private String isbn;                 // e.g., "978-1234567890" (only for Books)
    private Integer pages;               // e.g., 350 (only for Books)
}

