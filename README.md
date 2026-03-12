# Product Catalog + Order Management System

A production-quality demo application built with **Java 21**, **Spring Boot 3**, and **MongoDB 8** showcasing **7 MongoDB design patterns** with an interactive web interface.

> **Perfect for:** 15-minute webinars, MongoDB workshops, and teaching document database concepts

## 💡 MongoDB's Guiding Principle

> ### **"Data that is accessed together should be stored together."**

This demo exemplifies MongoDB's fundamental data modeling principle:

- **Embedding** - OrderItems stored with Orders (always accessed together) → Zero joins!
- **Subset Pattern** - Customer name stored with Orders (frequently accessed together) → Fast queries!
- **References** - Full customer/product data (occasionally accessed together) → Flexibility!

**Result:** 100x faster queries, simpler code, better performance! 🚀

## 🎯 Purpose

This project demonstrates:
- MongoDB's guiding principle: "Data accessed together, stored together"
- MongoDB document storage with JSON-like documents
- Natural mapping between Java objects and MongoDB documents
- REST API implementation with Spring Boot
- **Seven MongoDB design patterns:**
  1. **Embedding Pattern** - OrderItems embedded in Orders
  2. **Subset Pattern** - Customer/Product data denormalized
  3. **Reference Pattern** - Links between collections
  4. **Computed Pattern** ⭐ - Pre-calculated order totals
  5. **Polymorphic Pattern** ⭐ - Different product types (Electronics, Clothing, Books)
  6. **Document Versioning** ⭐ - Schema evolution tracking
  7. **Outlier Pattern** 🆕 - Handling large arrays (100+ items) with bucketing

## 🌐 Interactive Web Interface 🆕

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
- **[SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md)** ⭐ - Seven MongoDB Schema Design Patterns
- **[OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md)** 🆕 - Handling large arrays (50+ items)
- **[DATA_MODELING_PRINCIPLE.md](DATA_MODELING_PRINCIPLE.md)** - Deep dive into "data together" principle
- **Code files** - All model classes have beginner-friendly comments

### 🎤 **For Presenters**
- **[WEBINAR_OUTLINE.md](WEBINAR_OUTLINE.md)** - 15-minute presentation script
- **[PRESENTER_CHECKLIST.md](PRESENTER_CHECKLIST.md)** - Pre-webinar setup checklist
- **[demo-commands.sh](demo-commands.sh)** - Automated demo script

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

### 2. Start MongoDB (if using local)

```bash
# Option 1: Docker (recommended)
docker run -d -p 27017:27017 --name mongodb mongo:8

# Option 2: Docker Compose
docker-compose up -d
```

### 3. Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

### ⚠️ Connection Issues?

If you see MongoDB connection timeout errors, see **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** for solutions.

**Quick fix:** Whitelist your IP address in MongoDB Atlas Network Access settings.

## 📚 Complete Documentation

This project includes comprehensive documentation:

- **[README.md](README.md)** (this file) - Quick start and overview
- **[DEMO_GUIDE.md](DEMO_GUIDE.md)** - Complete API walkthrough with curl examples
- **[BEGINNERS_GUIDE.md](BEGINNERS_GUIDE.md)** - MongoDB concepts for beginners
- **[WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md)** - Interactive web interface guide
- **[SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md)** - Seven MongoDB design patterns
- **[OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md)** - Handling large arrays
- **[DATA_MODELING_PRINCIPLE.md](DATA_MODELING_PRINCIPLE.md)** - Core data modeling principles
- **[WEBINAR_OUTLINE.md](WEBINAR_OUTLINE.md)** - 15-minute presentation script
- **[PRESENTER_CHECKLIST.md](PRESENTER_CHECKLIST.md)** - Pre-webinar checklist
- **[PROJECT_STRUCTURE.txt](PROJECT_STRUCTURE.txt)** - Complete project structure

## 🏗️ Project Structure

```
src/main/java/com/example/store/
├── model/
│   ├── Customer.java      # Customer entity
│   ├── Product.java       # Product entity
│   ├── Order.java         # Order entity
│   └── OrderItem.java     # Embedded document
├── repository/
│   ├── CustomerRepository.java
│   ├── ProductRepository.java
│   └── OrderRepository.java
├── controller/
│   ├── CustomerController.java
│   ├── ProductController.java
│   └── OrderController.java
└── DemoApplication.java   # Main application
```

## 🧪 Quick Test

```bash
# Create a product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":1299.99,"category":"Electronics","inventory":50}'

# Get all products
curl http://localhost:8080/products
```

## 📖 Key Technologies

- **Java 21** - Latest LTS version
- **Spring Boot 3.2** - Application framework
- **Spring Data MongoDB** - MongoDB integration
- **MongoDB 8** - Document database
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

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
   - v1: `customer` (string)
   - v2: `customerId` + `customerName` (Subset Pattern)
   - v3: Added Outlier Pattern fields
   - Safe gradual migration

7. **Outlier Pattern** 🆕: Handle large arrays gracefully
   - Normal orders (< 50 items): Embed items in Order
   - Large orders (100+ items): Split into buckets
   - Optimizes for common case (99% of orders)
   - Handles outliers without hitting 16MB limit

**See [SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md) and [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for detailed explanations!**

## 🎓 Ideal For

- Webinar demonstrations (15-minute live coding)
- MongoDB introduction workshops
- Spring Boot + MongoDB tutorials
- Document database modeling examples

## 📝 License

Educational demo project - free to use and modify.

