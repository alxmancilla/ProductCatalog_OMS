package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Customer entity - represents a customer in the system.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This is a SEPARATE collection (not embedded)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This class becomes a "customers" collection in MongoDB.
 * Each customer is stored as a separate document.
 *
 * Why separate?
 * - One customer can have many orders
 * - Customer data (email, phone) changes independently of orders
 * - We don't want to duplicate all customer data in every order
 *
 * How do Orders connect to Customers?
 * - Orders store the customer's ID (customerId)
 * - Orders also copy the customer's name (for fast display)
 * - This is the "Subset Pattern" - copy what you need frequently!
 *
 * Example MongoDB document:
 * {
 *   "_id": "cust123",
 *   "name": "John Doe",
 *   "email": "john@example.com",
 *   "phone": "+1-555-0123"
 * }
 */
@Data                                    // Lombok: generates getters/setters
@NoArgsConstructor                       // Lombok: generates no-args constructor
@AllArgsConstructor                      // Lombok: generates all-args constructor
@Document(collection = "customers")      // MongoDB: stores in "customers" collection
public class Customer {

    @Id                                  // MongoDB: this is the document ID (_id)
    private String id;

    private String name;                 // Customer's full name

    private String email;                // Customer's email address

    private String phone;                // Customer's phone number
}

