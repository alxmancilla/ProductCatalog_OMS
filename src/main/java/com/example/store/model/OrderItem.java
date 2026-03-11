package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * OrderItem - represents one item in an order.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This is an EMBEDDED document (stored inside Order)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Notice: This class has NO @Document annotation!
 * Why? Because OrderItems are NOT stored in their own collection.
 * They are stored INSIDE the Order document.
 *
 * Think of it like this:
 * - Order = A box 📦
 * - OrderItems = Things inside the box 📄📄📄
 * - The box and its contents are stored together!
 *
 * ───────────────────────────────────────────────────────────────────────────
 * WHY EMBED ORDER ITEMS?
 * ───────────────────────────────────────────────────────────────────────────
 *
 * Question: When you view an order, do you ALWAYS need to see the items?
 * Answer: YES! ✅
 *
 * Solution: Store items INSIDE the order
 *
 * Benefit: One query gets the complete order with all items!
 *
 * ───────────────────────────────────────────────────────────────────────────
 * WHY COPY PRODUCT NAME AND PRICE?
 * ───────────────────────────────────────────────────────────────────────────
 *
 * Imagine: You bought a laptop for $1299.99 in January.
 * In February, the price changes to $1199.99.
 *
 * Question: What price should your order show?
 * Answer: $1299.99 (the price when you ordered!) ✅
 *
 * Solution: Copy the product name and price into the order item
 *
 * Benefit: Order history shows the correct price, even if product price changes!
 */
@Data                                    // Lombok: generates getters/setters
@NoArgsConstructor                       // Lombok: generates no-args constructor
@AllArgsConstructor                      // Lombok: generates all-args constructor
public class OrderItem {

    // 🔗 SUBSET PATTERN: Link + snapshot of product data
    private String productId;            // Link to current Product (if we need latest info)
    private String name;                 // Product name at order time (snapshot)
    private BigDecimal price;            // Product price at order time (snapshot)

    private Integer quantity;            // How many were ordered
}

