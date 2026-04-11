# Product Catalog + Order Management System

A production-quality demo application built with **Java 21**, **Spring Boot 3**, and **MongoDB 8** showcasing **8 MongoDB design patterns**, **ACID transactions**, and **complete OMS P0 features** with an interactive web interface.

> **Perfect for:** 15-minute webinars, MongoDB workshops, and teaching document database concepts
> **NEW:** Full Order Management System with status tracking, search, updates, and cancellation! 🎉

## 💡 MongoDB's Guiding Principle

> ### **"Data that is accessed together should be stored together."**

This demo exemplifies MongoDB's fundamental data modeling principle:

- **Embedding** - OrderItems stored with Orders (always accessed together) → Zero joins!
- **Subset Pattern** - Customer name stored with Orders (frequently accessed together) → Fast queries!
- **References** - Full customer/product data (occasionally accessed together) → Flexibility!
- **Transactions** 🆕 - ACID guarantees for order + inventory updates → Data consistency!

**Result:** 100x faster queries, simpler code, better performance, guaranteed consistency! 🚀

## 🎯 Purpose

This project demonstrates:
- MongoDB's guiding principle: "Data accessed together, stored together"
- MongoDB document storage with JSON-like documents
- Natural mapping between Java objects and MongoDB documents
- REST API implementation with Spring Boot
- **Complete Order Management System (OMS)** 🆕 with all P0 features
- **ACID transactions** for inventory management and order operations
- **Eight MongoDB design patterns:**
  1. **Embedding Pattern** - OrderItems + StatusHistory embedded in Orders
  2. **Subset Pattern** - Customer/Product data denormalized
  3. **Reference Pattern** - Links between collections
  4. **Computed Pattern** ⭐ - Pre-calculated order totals
  5. **Polymorphic Pattern** ⭐ - Different product types (Electronics, Clothing, Books)
  6. **Document Versioning** ⭐ - Schema evolution tracking
  7. **Outlier Pattern** ⭐ - Handling large arrays (100+ items) with bucketing
  8. **Transaction Pattern** 🆕 - ACID transactions for multi-document updates

## 📦 Product Dataset Import

Load all 222 products from `products-dataset.json` in one step — idempotent (safe to run multiple times):

**CLI:**
```bash
./import-products.sh                        # localhost:8080, products-dataset.json
./import-products.sh http://host:8080       # custom host
./import-products.sh http://host:8080 /path/to/products.json
```

**curl:**
```bash
curl -X POST http://localhost:8080/products/import \
     -F "file=@products-dataset.json"
```

**Web UI:** Products → **Import Dataset** → pick file → Upload & Import

**Response:**
```json
{ "imported": 222, "skipped": 0, "errors": [] }
```

Products whose SKU already exists are skipped automatically.

---

## 🚀 Order Management System (OMS) Features

All **Priority 0 (P0)** features are fully implemented and tested:

| Feature | Status | Endpoint | Description |
|---------|--------|----------|-------------|
| ✅ **Dataset Import** | Complete | `POST /products/import` | Bulk-load products from JSON file (CLI + UI) |
| ✅ **Order Creation** | Complete | `POST /orders` | Create orders with inventory validation |
| ✅ **Status Management** | Complete | `PUT /orders/{id}/status` | Update status with audit trail |
| ✅ **Get by ID** | Complete | `GET /orders/{id}` | Retrieve order with all items |
| ✅ **Search & Filter** | Complete | `GET /orders?...` | Search by customer, status, date, total |
| ✅ **Order Updates** | Complete | `PUT /orders/{id}/items` | Modify items with delta calculation, remove items with `quantity: 0` |
| ✅ **Order Cancellation** | Complete | `POST /orders/{id}/cancel` | Cancel with inventory restoration |
| ✅ **Analytics** 🆕 | Complete | `GET /analytics/orders/...` | Business intelligence using Aggregation Framework |

**See [`OMS_P0_TESTING_GUIDE.md`](OMS_P0_TESTING_GUIDE.md) for complete testing documentation.**

## 📊 Analytics & Business Intelligence 🆕

**Powered by MongoDB's Aggregation Framework!** Demonstrates advanced data analytics capabilities:

| Analytics Endpoint | Description | MongoDB Features |
|-------------------|-------------|------------------|
| `GET /analytics/orders/revenue-by-status` | Revenue breakdown by order status | `$group`, `$sum`, `$sort` |
| `GET /analytics/orders/top-customers?limit=10` | Top spending customers | `$group`, `$first`, `$limit` |
| `GET /analytics/orders/popular-products?limit=10` | Best-selling products | `$unwind`, `$addFields`, `$multiply` |
| `GET /analytics/orders/daily-revenue?days=30` | Daily revenue trends | `$dateToString`, time-series |

**Example - Revenue by Status:**
```bash
curl http://localhost:8080/analytics/orders/revenue-by-status
```

**Response:**
```json
[
  {
    "status": "DELIVERED",
    "totalRevenue": 125000.50,
    "orderCount": 450,
    "averageOrderValue": 277.78
  },
  {
    "status": "CONFIRMED",
    "totalRevenue": 85000.25,
    "orderCount": 320,
    "averageOrderValue": 265.63
  }
]
```

**MongoDB Aggregation Pipeline (vs SQL):**
- **$group** = `GROUP BY` + aggregation functions
- **$match** = `WHERE` clause filtering
- **$sort** = `ORDER BY` sorting
- **$unwind** = Flatten embedded arrays (unique to document databases!)
- **$addFields** = Computed fields during aggregation
- **$dateToString** = Date formatting and grouping

**Perfect for:** Executive dashboards, BI reports, revenue forecasting, customer segmentation! 📈

**See [`OrderAnalyticsController.java`](src/main/java/com/example/store/controller/OrderAnalyticsController.java) for detailed examples.**

## 🌐 Interactive Web Interface

**No curl commands needed!** This demo includes a beautiful web interface to test all API endpoints:

```bash
# Start the application
mvn spring-boot:run

# Open in your browser
http://localhost:8080
```

**Features:**
- ✨ Test all endpoints with a single click
- 🎨 Beautiful UI with color-coded HTTP methods
- 📋 Pattern badges showing which MongoDB patterns are demonstrated
- ⚡ Auto-generate large orders (150 items) for Outlier Pattern demo
- 🚀 Perfect for webinars and presentations!

**See [WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md) for details.**

---

## 📚 Documentation for Different Audiences

### 🎓 **New to MongoDB? Start Here!**
1. **[BEGINNERS_GUIDE.md](BEGINNERS_GUIDE.md)** - MongoDB concepts explained simply
2. **[WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md)** 🆕 - Use the interactive web interface
3. **[DEMO_GUIDE.md](DEMO_GUIDE.md)** - Complete API walkthrough with examples

### 👨‍💻 **For Developers**
- **[SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md)** ⭐ - Eight MongoDB Schema Design Patterns
- **[TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md)** 🆕 - ACID transactions for inventory management
- **[OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md)** - Handling large arrays (100+ items)
- **[PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md)** 🆕 - Product schema evolution (v1 → v2)
- **[DATA_MODELING_PRINCIPLE.md](DATA_MODELING_PRINCIPLE.md)** - Deep dive into "data together" principle
- **[VALIDATION_ARCHITECTURE.md](VALIDATION_ARCHITECTURE.md)** - Defense-in-depth validation
- **Code files** - All model classes have beginner-friendly comments

### 🎤 **For Presenters**
- **[WEBINAR_OUTLINE.md](WEBINAR_OUTLINE.md)** - 15-minute presentation script
- **[PRESENTER_CHECKLIST.md](PRESENTER_CHECKLIST.md)** - Pre-webinar setup checklist
- **[demo-commands.sh](demo-commands.sh)** - Automated demo script
- **[test-transactions.sh](test-transactions.sh)** 🆕 - Transaction testing script
- **[import-products.sh](import-products.sh)** - Bulk import products-dataset.json via API

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- MongoDB 8 (Atlas or local)

### 1. Configure Database Connection

**First time setup:**
```bash
# Copy the template file
cp src/main/resources/application.properties.template src/main/resources/application.properties

# Edit application.properties and add your MongoDB credentials
# For MongoDB Atlas: Replace <db_username> and <db_password>
# For local MongoDB: Use mongodb://localhost:27017
```

**Important Security Notes:**
- ✅ The `application.properties` file is in `.gitignore` to protect your credentials
- ✅ Never commit database passwords or API keys to version control
- ✅ Use the template file (`application.properties.template`) as a reference
- ✅ Each developer should create their own `application.properties` locally

### 2. Start MongoDB with Replica Set (Required for Transactions)

**Important:** MongoDB transactions require a replica set configuration!

```bash
# Option 1: Docker Compose (recommended - includes replica set)
docker-compose up -d

# Wait for replica set initialization (automatic via healthcheck)
# Check status:
mongosh
> rs.status()

# Option 2: Docker (manual replica set setup)
docker run -d -p 27017:27017 --name mongodb \
  mongo:8 --replSet rs0 --bind_ip_all

# Initialize replica set:
mongosh
> rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]})
```

**Why replica set?** MongoDB transactions require the replica set's oplog for ACID guarantees. Even a single-node replica set works for development!

### 3. Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

### 4. Import the Product Dataset

```bash
./import-products.sh   # loads all 222 products into MongoDB
```

Or open `http://localhost:8080` → Products → **Import Dataset** and upload `products-dataset.json`.

### ⚠️ Connection Issues?

If you see MongoDB connection timeout errors, see **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** for solutions.

**Quick fix:** Whitelist your IP address in MongoDB Atlas Network Access settings.

## 📚 Complete Documentation

This project includes comprehensive documentation:

### 📖 Core Documentation
- **[README.md](README.md)** (this file) - Quick start and overview
- **[DEMO_GUIDE.md](DEMO_GUIDE.md)** - Complete API walkthrough with curl examples
- **[BEGINNERS_GUIDE.md](BEGINNERS_GUIDE.md)** - MongoDB concepts for beginners
- **[WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md)** - Interactive web interface guide

### 🎨 Design Patterns & Transactions
- **[SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md)** - Eight MongoDB design patterns
- **[TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md)** 🆕 - ACID transactions for inventory management
- **[OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md)** - Handling large arrays (100+ items)
- **[PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md)** 🆕 - Product schema evolution
- **[DATA_MODELING_PRINCIPLE.md](DATA_MODELING_PRINCIPLE.md)** - Core data modeling principles
- **[VALIDATION_ARCHITECTURE.md](VALIDATION_ARCHITECTURE.md)** - Defense-in-depth validation

### 🎤 Presentation
- **[WEBINAR_OUTLINE.md](WEBINAR_OUTLINE.md)** - 15-minute presentation script
- **[PRESENTER_CHECKLIST.md](PRESENTER_CHECKLIST.md)** - Pre-webinar checklist

### 🔧 Reference
- **[PROJECT_STRUCTURE.txt](PROJECT_STRUCTURE.txt)** - Complete project structure
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and solutions
- **[TRANSACTION_IMPLEMENTATION_SUMMARY.md](TRANSACTION_IMPLEMENTATION_SUMMARY.md)** 🆕 - Transaction implementation details

## 🏗️ Project Structure

```
src/main/java/com/example/store/
├── model/
│   ├── Customer.java          # Customer entity
│   ├── Product.java           # Product entity (with inventory)
│   ├── Order.java             # Order entity (with versioning)
│   ├── OrderItem.java         # Embedded document
│   └── OrderItemBucket.java   # Outlier pattern for large orders
├── repository/
│   ├── CustomerRepository.java
│   ├── ProductRepository.java  # findBySku() for idempotent import
│   ├── OrderRepository.java
│   └── OrderItemBucketRepository.java
├── service/
│   ├── ProductImportService.java     # Bulk JSON import (skips existing SKUs)
│   └── OrderTransactionService.java  # 🆕 Transaction service
├── exception/
│   ├── InsufficientInventoryException.java  # 🆕 Inventory errors
│   └── ProductNotFoundException.java        # 🆕 Product errors
├── controller/
│   ├── CustomerController.java
│   ├── ProductController.java  # POST /products/import endpoint
│   └── OrderController.java   # Uses transactions
├── config/
│   ├── MongoConfig.java       # 🆕 Transaction manager
│   └── MongoSchemaValidation.java
└── DemoApplication.java       # Main application
```

## 🧪 Quick Test

### Import the Full Product Dataset
```bash
# Loads all 222 products — idempotent, safe to re-run
./import-products.sh

# Expected response:
# { "imported": 222, "skipped": 0, "errors": [] }
```

### Test Basic CRUD
```bash
# Create a product with inventory
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "description": "High-performance laptop",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 10,
    "sku": "LAPTOP-001"
  }'

# Get all products
curl http://localhost:8080/products
```

### Test Transactions & Inventory 🆕
```bash
# Run the automated transaction test script
./test-transactions.sh

# Or test manually:
# 1. Create an order (inventory will be decremented)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 2
      }
    ]
  }'

# 2. Check inventory was decremented
curl http://localhost:8080/products/PRODUCT_ID
# inventory should now be 8 (10 - 2)

# 3. Update order items (remove item by setting quantity to 0)
curl -X PUT http://localhost:8080/orders/ORDER_ID/items \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 0
      }
    ],
    "updatedBy": "customer@example.com",
    "reason": "Customer removed item"
  }'
# Item with quantity 0 is removed, inventory restored to 10

# 4. Try to order more than available (should fail)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "customerName": "Jane Smith",
    "items": [
      {
        "productId": "PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 100
      }
    ]
  }'
# Returns 400 Bad Request with inventory error details
```

## 📖 Key Technologies

- **Java 21** - Latest LTS version
- **Spring Boot 3.2** - Application framework
- **Spring Data MongoDB** - MongoDB integration with transaction support
- **MongoDB 8** - Document database with ACID transactions
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

## 🔄 Transaction Features 🆕

This application demonstrates **MongoDB ACID transactions** for inventory management across **ALL order types**:

### **What Transactions Provide**
- ✅ **Atomicity** - Order creation + inventory updates succeed or fail together
- ✅ **Consistency** - Data always in valid state
- ✅ **Isolation** - Concurrent orders don't interfere
- ✅ **Durability** - Committed changes are permanent

### **Use Case: Standard Order Creation**
```
START TRANSACTION
├─ 1. Validate customer exists
├─ 2. Validate all products exist
├─ 3. Check inventory availability
├─ 4. Create order document
├─ 5. Decrement inventory for all products
└─ COMMIT (if all succeed) or ROLLBACK (if any fail)
```

### **Use Case: Large Order Creation (100+ items)** 🆕
```
START TRANSACTION
├─ 1. Validate customer exists
├─ 2. Validate all products exist
├─ 3. Check inventory availability
├─ 4. Create order document (items = null)
├─ 5. Create all bucket documents
├─ 6. Decrement inventory for all products
└─ COMMIT (if all succeed) or ROLLBACK (if any fail)
```

### **Benefits**
- 🛡️ **No Overselling** - Can't sell more than available
- 👤 **Referential Integrity** - Orders can't reference non-existent customers
- 📦 **Product Validation** - All products must exist before order creation
- 🔄 **Automatic Rollback** - Failed operations don't leave partial data
- 🏃 **Race Condition Prevention** - Concurrent orders handled correctly
- 💬 **Clear Error Messages** - Users know exactly what went wrong
- ⚖️ **Consistent Validation** - Same checks for standard AND large orders

**See [TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md) for complete documentation.**

## ⚙️ Configuration Highlights

### Clean JSON Responses
The application is configured to exclude `null` fields from JSON responses for cleaner, more professional API output:

```properties
# application.properties
spring.jackson.default-property-inclusion=non_null
```

**Before:**
```json
{
  "id": "123",
  "name": "Laptop",
  "price": 1299.99,
  "warranty": "2 years",
  "brand": "TechCorp",
  "size": null,        ← Excluded
  "color": null,       ← Excluded
  "material": null     ← Excluded
}
```

**After:**
```json
{
  "id": "123",
  "name": "Laptop",
  "price": 1299.99,
  "warranty": "2 years",
  "brand": "TechCorp"
}
```

This is especially useful for the **Polymorphic Pattern** where different product types have different fields!

### BigDecimal → Decimal128 Conversion

The application properly stores monetary values (`BigDecimal` in Java) as MongoDB's native `Decimal128` type:

**Java Code:**
```java
private BigDecimal price;  // e.g., 1299.99
private BigDecimal total;  // e.g., 1359.97
```

**MongoDB Storage (Product):**
```json
{
  "name": "Laptop",
  "price": NumberDecimal("1299.99")  // Stored as Decimal128, not string!
}
```

**MongoDB Storage (Order with embedded items):**
```json
{
  "customerName": "John Doe",
  "total": NumberDecimal("1359.97"),
  "items": [
    {
      "name": "Laptop",
      "price": NumberDecimal("1299.99"),  // Also Decimal128 in embedded docs!
      "quantity": 1
    }
  ]
}
```

**Benefits:**
- ✅ Stored as actual numbers (not strings)
- ✅ Enables numeric queries and aggregations in MongoDB
- ✅ Preserves precision (128-bit decimal)
- ✅ Shows correctly in MongoDB Compass and shell
- ✅ Works for both top-level and embedded documents

**Applies to:**
- `Product.price`
- `OrderItem.price` (embedded in Order)
- `Order.total`

**Implementation:** See `MongoConfig.java` for custom BigDecimal ↔ Decimal128 converters.

## 💡 Learning Highlights

### Embedded Documents
This demo showcases MongoDB's embedded document pattern where `OrderItem` objects are stored directly within `Order` documents, demonstrating:
- Single-query retrieval
- Atomic operations
- Data locality
- Natural data modeling

### MongoDB Design Patterns Demonstrated

This demo showcases **SEVEN MongoDB design patterns**:

#### Core Patterns (Data Modeling)

1. **Embedding Pattern**: OrderItems embedded within Orders
   - Data accessed together
   - Atomic updates
   - Single query retrieval

2. **Referencing Pattern**: Orders reference Customers and Products
   - Shared data across documents
   - Single source of truth
   - Avoid data duplication for mutable fields

3. **Subset Pattern**: Orders store customer/product ID + frequently accessed fields
   - `customerId` + `customerName` (subset of customer data)
   - `productId` + `name`/`price` (subset of product data)
   - Optimizes read performance
   - Preserves historical accuracy

#### Schema Design Patterns ⭐ NEW!

4. **Computed Pattern**: Pre-calculate and store values
   - Order `total` calculated from items
   - Fast reads (no recalculation needed)
   - Consistent results

5. **Polymorphic Pattern**: Variable fields per document type
   - Electronics products have `warranty` and `brand`
   - Clothing products have `size`, `color`, `material`
   - Book products have `author`, `isbn`, `pages`
   - All in ONE collection!

6. **Document Versioning**: Track schema evolution
   - `schemaVersion` field tracks changes
   - **Orders**: v1 → v2 (Subset Pattern) → v3 (Outlier Pattern)
   - **Products**: v1 (basic) → v2 (inventory + SKU + description) 🆕
   - Safe gradual migration
   - See [PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md) for details

7. **Outlier Pattern** 🆕: Handle large arrays gracefully
   - Normal orders (< 100 items): Embed items in Order
   - Large orders (100+ items): Split into buckets, `items` field set to `null`
   - Optimizes for common case (99% of orders)
   - Handles outliers without hitting 16MB limit
   - Schema validation allows `items` to be `null` for large orders 🆕

**See [SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md) and [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for detailed explanations!**

## 🎓 Ideal For

- Webinar demonstrations (15-minute live coding)
- MongoDB introduction workshops
- Spring Boot + MongoDB tutorials
- Document database modeling examples

## 📝 License

Educational demo project - free to use and modify.

