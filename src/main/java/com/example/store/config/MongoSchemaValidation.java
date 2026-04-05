package com.example.store.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequiredArgsConstructor
public class MongoSchemaValidation implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MongoSchemaValidation.class);

    private final MongoTemplate mongoTemplate;

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
     * 🎯 DEMONSTRATES: POLYMORPHIC PATTERN with COMPOSITION + JSON Schema Validation
     *
     * This validation uses MongoDB's `oneOf` to enforce type-specific validation:
     * - Electronics products MUST have electronicsDetails with warranty and brand
     * - Clothing products MUST have clothingDetails with size, color, and material
     * - Book products MUST have bookDetails with author, isbn, and pages
     *
     * Benefits:
     * - Server-side validation ensures data integrity
     * - Type-specific fields are validated based on product type
     * - Prevents invalid product documents from being stored
     * - Complements application-layer validation (Strategy Pattern)
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

        // Define common fields that all products must have
        Document commonProperties = new Document()
            .append("name", new Document()
                .append("bsonType", "string")
                .append("minLength", 1)
                .append("description", "Product name is required"))
            .append("description", new Document()
                .append("bsonType", "string")
                .append("minLength", 1)
                .append("description", "Product description is required"))
            .append("price", new Document()
                .append("bsonType", java.util.Arrays.asList("double", "decimal"))
                .append("minimum", 0.01)
                .append("description", "Product price must be positive"))
            .append("category", new Document()
                .append("bsonType", "string")
                .append("minLength", 1))
            .append("inventory", new Document()
                .append("bsonType", "int")
                .append("minimum", 0))
            .append("sku", new Document()
                .append("bsonType", "string")
                .append("minLength", 1));

        // Define electronicsDetails schema
        Document electronicsDetailsSchema = new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("warranty", "brand"))
            .append("properties", new Document()
                .append("warranty", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Warranty is required for electronics (e.g., '1 year', '2 years')"))
                .append("brand", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Brand is required for electronics"))
                .append("weight", new Document()
                    .append("bsonType", "string"))
                .append("screenSize", new Document()
                    .append("bsonType", "string"))
                .append("resolution", new Document()
                    .append("bsonType", "string"))
                .append("capacity", new Document()
                    .append("bsonType", "string"))
            );

        // Define clothingDetails schema
        Document clothingDetailsSchema = new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("size", "color", "material"))
            .append("properties", new Document()
                .append("size", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Size is required for clothing"))
                .append("color", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Color is required for clothing"))
                .append("material", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Material is required for clothing"))
            );

        // Define bookDetails schema
        Document bookDetailsSchema = new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("author", "isbn", "pages"))
            .append("properties", new Document()
                .append("author", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "Author is required for books"))
                .append("isbn", new Document()
                    .append("bsonType", "string")
                    .append("minLength", 1)
                    .append("description", "ISBN is required for books"))
                .append("pages", new Document()
                    .append("bsonType", "int")
                    .append("minimum", 1)
                    .append("description", "Page count is required for books"))
                .append("publisher", new Document()
                    .append("bsonType", "string"))
                .append("language", new Document()
                    .append("bsonType", "string"))
            );

        // Create polymorphic validation using oneOf
        // This ensures that based on the "type" field, the appropriate detail object is required
        // SUPPORTS: Electronics, Clothing, Book (with detail objects), and General (without detail objects)
        Document validator = new Document("$jsonSchema", new Document()
            .append("bsonType", "object")
            .append("required", java.util.Arrays.asList("name", "description", "price", "category", "inventory", "sku"))
            .append("properties", commonProperties
                .append("type", new Document()
                    .append("bsonType", "string")
                    .append("enum", java.util.Arrays.asList("Electronics", "Clothing", "Book", "General"))
                    .append("description", "Product type must be Electronics, Clothing, Book, or General"))
                .append("electronicsDetails", electronicsDetailsSchema)
                .append("clothingDetails", clothingDetailsSchema)
                .append("bookDetails", bookDetailsSchema)
                .append("descriptionEmbedding", new Document()
                    .append("bsonType", "array")
                    .append("description", "Vector embedding for hybrid search"))
            )
            // Use oneOf to enforce type-specific validation
            .append("oneOf", java.util.Arrays.asList(
                // Electronics: type=Electronics AND electronicsDetails exists
                new Document()
                    .append("properties", new Document()
                        .append("type", new Document().append("enum", java.util.Arrays.asList("Electronics"))))
                    .append("required", java.util.Arrays.asList("electronicsDetails")),
                // Clothing: type=Clothing AND clothingDetails exists
                new Document()
                    .append("properties", new Document()
                        .append("type", new Document().append("enum", java.util.Arrays.asList("Clothing"))))
                    .append("required", java.util.Arrays.asList("clothingDetails")),
                // Book: type=Book AND bookDetails exists
                new Document()
                    .append("properties", new Document()
                        .append("type", new Document().append("enum", java.util.Arrays.asList("Book"))))
                    .append("required", java.util.Arrays.asList("bookDetails")),
                // General: type=General AND no detail objects required
                new Document()
                    .append("properties", new Document()
                        .append("type", new Document().append("enum", java.util.Arrays.asList("General"))))
                    // No required detail objects for General products
            ))
        );

        // Create collection with validation
        ValidationOptions validationOptions = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);

        database.createCollection("products",
            new com.mongodb.client.model.CreateCollectionOptions()
                .validationOptions(validationOptions));

        logger.info("✅ Products collection created with polymorphic schema validation (Composition Pattern)");
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

