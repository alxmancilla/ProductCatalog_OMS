package com.example.store.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * MongoDB Schema Validation Configuration.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Enforce data quality at the database level
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This class sets up MongoDB's built-in schema validation using JSON Schema.
 * 
 * Why use MongoDB Schema Validation?
 * - Database-level validation (works even outside Spring Boot)
 * - Prevents invalid data from any source (API, mongosh, Compass, etc.)
 * - Complements Spring Boot validation (@Valid, @NotBlank, etc.)
 * - Documents your schema in the database itself
 * - Can be viewed in MongoDB Compass
 * 
 * Validation Levels:
 * - STRICT: Validate all inserts and updates (recommended for new collections)
 * - MODERATE: Validate inserts and updates to valid documents (allows fixing invalid data)
 * 
 * Validation Actions:
 * - ERROR: Reject invalid documents (recommended for production)
 * - WARN: Log warnings but allow invalid documents (useful for migration)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This runs once at application startup
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * The @Component annotation makes this a Spring bean.
 * The CommandLineRunner interface means the run() method executes at startup.
 * This sets up validation rules before any data operations occur.
 */
@Component
public class MongoSchemaValidation implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MongoSchemaValidation.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        logger.info("Setting up MongoDB schema validation...");
        
        try {
            setupProductValidation();
            setupCustomerValidation();
            setupOrderValidation();
            
            logger.info("MongoDB schema validation setup complete!");
        } catch (Exception e) {
            logger.error("Failed to setup MongoDB schema validation", e);
            // Don't fail application startup if validation setup fails
            // This allows the app to run even if validation is already configured
        }
    }

    /**
     * Set up schema validation for the products collection.
     * 
     * Validates:
     * - name: required, non-empty string
     * - description: required, non-empty string
     * - price: required, positive number
     * - category: required, non-empty string
     * - inventory: required, non-negative integer
     */
    private void setupProductValidation() {
        MongoDatabase database = mongoTemplate.getDb();
        
        // Check if collection exists
        boolean collectionExists = database.listCollectionNames()
                .into(new java.util.ArrayList<>())
                .contains("products");
        
        if (collectionExists) {
            // Drop and recreate with validation
            logger.info("Updating products collection with schema validation...");
            database.getCollection("products").drop();
        }
        
        // Create validation schema using JSON Schema
        Document validator = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("name", "description", "price", "category", "inventory", "sku"))
            .append("properties", new Document()
                .append("name", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Product name is required and must be a non-empty string"))
                .append("description", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Product description is required and must be a non-empty string"))
                .append("price", new Document()
                    .append("bsonType", java.util.Arrays.asList("double", "decimal"))
                    .append("minimum", 0.01)
                    .append("description", "Product price is required and must be a positive number"))
                .append("category", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Product category is required and must be a non-empty string"))
                .append("inventory", new Document()
                    .append("bsonType", "int")
                    .append("minimum", 0)
                    .append("description", "Product inventory is required and must be a non-negative integer"))
                .append("sku", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Product SKU is required and must be a non-empty string"))
                .append("type", new Document()
                    .append("bsonType", "string")
                    .append("description", "Product type (for Polymorphic Pattern)"))
                // Electronics-specific fields (optional)
                .append("brand", new Document()
                    .append("bsonType", "string"))
                .append("warranty", new Document()
                    .append("bsonType", "string"))
                .append("weight", new Document()
                    .append("bsonType", "string"))
                .append("screenSize", new Document()
                    .append("bsonType", "string"))
                .append("resolution", new Document()
                    .append("bsonType", "string"))
                .append("capacity", new Document()
                    .append("bsonType", "string"))
                // Clothing-specific fields (optional)
                .append("size", new Document()
                    .append("bsonType", "string"))
                .append("color", new Document()
                    .append("bsonType", "string"))
                .append("material", new Document()
                    .append("bsonType", "string"))
                // Book-specific fields (optional)
                .append("author", new Document()
                    .append("bsonType", "string"))
                .append("isbn", new Document()
                    .append("bsonType", "string"))
                .append("pages", new Document()
                    .append("bsonType", "int"))
                .append("publisher", new Document()
                    .append("bsonType", "string"))
                .append("language", new Document()
                    .append("bsonType", "string"))
            )
        );

        // Create collection with validation
        ValidationOptions validationOptions = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);

        database.createCollection("products",
            new com.mongodb.client.model.CreateCollectionOptions()
                .validationOptions(validationOptions));

        logger.info("✅ Products collection created with schema validation");
    }

    /**
     * Set up schema validation for the customers collection.
     *
     * Validates:
     * - name: required, non-empty string
     * - email: optional, but must be string if present
     * - phone: optional, but must be string if present
     */
    private void setupCustomerValidation() {
        MongoDatabase database = mongoTemplate.getDb();

        // Check if collection exists
        boolean collectionExists = database.listCollectionNames()
                .into(new java.util.ArrayList<>())
                .contains("customers");

        if (collectionExists) {
            logger.info("Updating customers collection with schema validation...");
            database.getCollection("customers").drop();
        }

        // Create validation schema
        Document validator = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("name"))
            .append("properties", new Document()
                .append("name", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Customer name is required and must be a non-empty string"))
                .append("email", new Document()
                    .append("bsonType", "string")
                    .append("description", "Customer email (optional)"))
                .append("phone", new Document()
                    .append("bsonType", "string")
                    .append("description", "Customer phone (optional)"))
            )
        );

        // Create collection with validation
        ValidationOptions validationOptions = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);

        database.createCollection("customers",
            new com.mongodb.client.model.CreateCollectionOptions()
                .validationOptions(validationOptions));

        logger.info("✅ Customers collection created with schema validation");
    }

    /**
     * Set up schema validation for the orders collection.
     *
     * Validates:
     * - customerId: required, non-empty string
     * - customerName: required, non-empty string
     * - items: OPTIONAL (null for large orders using Outlier Pattern)
     * - total: required, positive number
     *
     * 🎯 OUTLIER PATTERN: For orders with 100+ items, the items field is set to null
     * and items are stored in the order_item_buckets collection instead.
     */
    private void setupOrderValidation() {
        MongoDatabase database = mongoTemplate.getDb();

        // Check if collection exists
        boolean collectionExists = database.listCollectionNames()
                .into(new java.util.ArrayList<>())
                .contains("orders");

        if (collectionExists) {
            logger.info("Updating orders collection with schema validation...");
            database.getCollection("orders").drop();
        }

        // Create validation schema
        // NOTE: 'items' is NOT in the required list to support the Outlier Pattern
        Document validator = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("customerId", "customerName", "total"))
            .append("properties", new Document()
                .append("customerId", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Customer ID is required"))
                .append("customerName", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Customer name is required (Subset Pattern)"))
                .append("items", new Document()
                    .append("bsonType", java.util.Arrays.asList("array", "null"))  // Allow null for Outlier Pattern
                    .append("description", "Order items array (null for large orders using Outlier Pattern)")
                    .append("items", new Document()
                        .append("bsonType", "object")
                        .append("required", java.util.Arrays.asList("productId", "name", "price", "quantity"))
                        .append("properties", new Document()
                            .append("productId", new Document()
                                .append("bsonType", "string"))
                            .append("name", new Document()
                                .append("bsonType", "string"))
                            .append("price", new Document()
                                .append("bsonType", java.util.Arrays.asList("double", "decimal"))
                                .append("minimum", 0.01))
                            .append("quantity", new Document()
                                .append("bsonType", "int")
                                .append("minimum", 1))
                        )
                    ))
                .append("total", new Document()
                    .append("bsonType", java.util.Arrays.asList("double", "decimal"))
                    .append("minimum", 0.01)
                    .append("description", "Order total is required and must be positive"))
                .append("isLargeOrder", new Document()
                    .append("bsonType", "bool")
                    .append("description", "Flag for Outlier Pattern"))
                .append("schemaVersion", new Document()
                    .append("bsonType", "int")
                    .append("description", "Schema version for Versioning Pattern"))
            )
        );

        // Create collection with validation
        ValidationOptions validationOptions = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);

        database.createCollection("orders",
            new com.mongodb.client.model.CreateCollectionOptions()
                .validationOptions(validationOptions));

        logger.info("✅ Orders collection created with schema validation");
    }
}

