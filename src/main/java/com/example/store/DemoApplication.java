package com.example.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application — Product Catalog + Order Management System Demo.
 *
 * This demo showcases eight MongoDB schema design patterns in a realistic
 * e-commerce context:
 *
 *   1. Embedding Pattern      — OrderItems stored inside Order documents
 *   2. Subset Pattern         — customerName denormalized into Order for fast reads
 *   3. Reference Pattern      — customerId links Order → Customer collection
 *   4. Computed Pattern       — Order.total pre-calculated and stored
 *   5. Polymorphic Pattern    — Electronics, Clothing, Books in one collection
 *   6. Document Versioning    — schemaVersion field tracks schema evolution
 *   7. Outlier Pattern        — large orders (100+ items) split into buckets
 *   8. Transaction Pattern    — ACID transactions for inventory management
 *
 * Additional features: MongoDB Aggregation Framework analytics, AI-powered
 * natural language order creation (Grove API + LangGraph), and vector/hybrid
 * search via Voyage AI embeddings.
 *
 * Entry point: http://localhost:8080 — opens the interactive web demo UI.
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

