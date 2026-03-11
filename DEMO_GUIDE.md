# Product Catalog + Order Management System Demo

## Webinar Demo Guide (15 minutes)

This is a simple demo application showcasing **Spring Boot + MongoDB** for building a Product Catalog and Order Management System.

---

## � **NEW! Interactive Web Interface** 🎉

**No curl commands needed!** This demo now includes a beautiful web interface to test all API endpoints.

### Quick Start (2 Steps)

```bash
# 1. Start the application
mvn spring-boot:run

# 2. Open your browser
http://localhost:8080
```

**That's it!** You now have a beautiful, interactive API tester with:
- ✨ One-click endpoint testing
- 🎨 Color-coded HTTP methods
- 📊 Pattern badges showing MongoDB design patterns
- ⚡ Auto-generate large orders (150 items) for Outlier Pattern demo
- 🚀 Perfect for webinars and presentations!

**📚 See [WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md) for complete details.**

---

### 💡 Prefer curl Commands?

This guide also includes traditional curl commands below for those who prefer command-line testing.

---

## �🎯 Demo Objectives

1. Show how MongoDB stores JSON-like documents
2. Demonstrate how Java objects map naturally to MongoDB documents
3. Implement a simple REST API
4. Demonstrate embedded documents in MongoDB
5. Show basic CRUD operations
6. **Exemplify MongoDB's guiding principle: "Data that is accessed together should be stored together"**

---

## 💡 MongoDB's Guiding Principle (Simple Explanation)

> ### **"Data that is accessed together should be stored together."**

**What does this mean?**

Think about viewing an order on a website:
- You see: Order date, customer name, items, total price
- You DON'T see: Customer's email, phone, or current product inventory

**MongoDB's approach:** Store what you see together in one document!

### 📊 Visual Example

**Traditional Database (SQL):**
```
orders table    →  JOIN  →  order_items table  →  JOIN  →  products table
customers table →  JOIN  →  orders table
```
Result: Multiple queries, slow performance

**MongoDB (This Demo):**
```json
{
  "orderId": "123",
  "customerName": "John Doe",        ← Stored together!
  "items": [                         ← Stored together!
    {
      "name": "Laptop",              ← Stored together!
      "price": 1299.99,
      "quantity": 1
    }
  ],
  "total": 1299.99
}
```
Result: One query, fast performance! 🚀

---

## 🎓 Three Simple Patterns (For Beginners)

### 1. **Embedding** = Store Everything Together
- **When?** Data is ALWAYS accessed together
- **Example:** Order items are always shown with the order
- **Benefit:** One query gets everything!

### 2. **Subset Pattern** = Store Some Data Together
- **When?** You FREQUENTLY need some fields, but not all
- **Example:** You often need customer name, but rarely need email/phone
- **Benefit:** Fast queries most of the time!

### 3. **Reference** = Store Link Only
- **When?** You RARELY need the related data
- **Example:** Full customer profile is only needed occasionally
- **Benefit:** Saves space, single source of truth!

---

## 🔍 How This Demo Uses These Patterns

### Pattern 1: Embedding (OrderItems in Orders)
```java
private List<OrderItem> items;  // Items stored INSIDE the order
```
**Why?** You ALWAYS need items when viewing an order ✅

### Pattern 2: Subset (Customer Name in Orders)
```java
private String customerId;      // Link to full customer data
private String customerName;    // Copy for quick display
```
**Why?** You FREQUENTLY need name, RARELY need email/phone ✅

### Pattern 3: Reference (Customer ID)
```java
private String customerId;      // Link to get full customer when needed
```
**Why?** Full customer data is OCCASIONALLY needed ✅

---

## 🛠️ Prerequisites

- **Java 21+**
- **Maven 3.6+**
- **MongoDB 8** (running locally on port 27017)

### Start MongoDB

```bash
# Using Docker (recommended for demo)
docker run -d -p 27017:27017 --name mongodb mongo:8

# Or using local MongoDB installation
mongod --dbpath /path/to/data
```

---

## 🚀 Running the Application

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

---

## 📦 API Endpoints

### Customers API

#### Create a Customer
```bash
POST /customers
```

#### Get All Customers
```bash
GET /customers
```

### Products API

#### Create a Product
```bash
POST /products
```

#### Get All Products
```bash
GET /products
```

### Orders API

#### Create an Order
```bash
POST /orders
```

#### Get All Orders
```bash
GET /orders
```

---

## 🧪 Testing the API

### Option 1: Web Interface (Recommended) 🌐

**The easiest way to test the API is using the web interface:**

1. Start the application: `mvn spring-boot:run`
2. Open your browser: `http://localhost:8080`
3. Click any endpoint in the sidebar
4. Click **"🚀 Send Request"**

**See [WEB_INTERFACE_GUIDE.md](WEB_INTERFACE_GUIDE.md) for a complete walkthrough.**

---

### Option 2: curl Commands (Traditional)

If you prefer command-line testing, here are the curl commands:

#### 1. Create a Customer

```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123"
  }'
```

**Note:** Save the customer `id` from the response - you'll need it for creating orders.

### 2. Create Products

```bash
# Create Product 1: Laptop
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50
  }'

# Create Product 2: Wireless Mouse
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "price": 29.99,
    "category": "Accessories",
    "inventory": 200
  }'

# Create Product 3: USB-C Cable
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "USB-C Cable 2m",
    "price": 15.99,
    "category": "Accessories",
    "inventory": 500
  }'
```

### 3. Get All Products

```bash
curl http://localhost:8080/products | jq
```

### 4. Create an Order

**Note:** Replace `<customer-id>`, `<product-id-1>`, and `<product-id-2>` with actual IDs from the customer and products you created.

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "<customer-id>",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "<product-id-1>",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 1
      },
      {
        "productId": "<product-id-2>",
        "name": "Wireless Mouse",
        "price": 29.99,
        "quantity": 2
      }
    ]
  }'
```

### 5. Get All Orders

```bash
curl http://localhost:8080/orders | jq
```

---

## 📄 Example MongoDB Documents

### Customer Document (in `customers` collection)

```json
{
  "_id": "65a1b2c3d4e5f6g7h8i9j0k0",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-0123",
  "_class": "com.example.store.model.Customer"
}
```

### Product Document (in `products` collection)

```json
{
  "_id": "65a1b2c3d4e5f6g7h8i9j0k1",
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "category": "Electronics",
  "inventory": 50,
  "_class": "com.example.store.model.Product"
}
```

### Order Document (in `orders` collection)

```json
{
  "_id": "65a1b2c3d4e5f6g7h8i9j0k2",
  "customerId": "65a1b2c3d4e5f6g7h8i9j0k0",
  "customerName": "John Doe",
  "orderDate": "2024-01-15T10:30:00",
  "items": [
    {
      "productId": "65a1b2c3d4e5f6g7h8i9j0k1",
      "name": "Laptop Pro 15",
      "price": 1299.99,
      "quantity": 1
    },
    {
      "productId": "65a1b2c3d4e5f6g7h8i9j0k3",
      "name": "Wireless Mouse",
      "price": 29.99,
      "quantity": 2
    }
  ],
  "total": 1359.97,
  "_class": "com.example.store.model.Order"
}
```

---

## 💡 "Data Accessed Together, Stored Together" - In Action

### Real-World Query Analysis

Let's analyze how data is actually accessed in an e-commerce system:

#### Common Query Patterns:

1. **"Show order details"** (90% of queries)
   - Need: Order date, customer name, items, total
   - Don't need: Customer email, phone, address
   - Don't need: Current product inventory

2. **"List all orders"** (80% of queries)
   - Need: Order ID, customer name, date, total
   - Don't need: Full customer profile
   - Don't need: Current product details

3. **"Get customer profile"** (10% of queries)
   - Need: Full customer data (name, email, phone, address)
   - Don't need: Order details

4. **"Update product inventory"** (5% of queries)
   - Need: Current product data
   - Don't need: Historical order data

### How This Demo Applies the Principle:

#### ✅ OrderItems EMBEDDED in Order
**Why?** Order items are ALWAYS accessed with the order.

```json
{
  "_id": "order123",
  "items": [                    // ← EMBEDDED (accessed together)
    {
      "productId": "prod456",
      "name": "Laptop",
      "price": 1299.99,
      "quantity": 1
    }
  ]
}
```

**Result:** One query retrieves complete order. No joins!

#### ✅ Customer Name STORED in Order (Subset Pattern)
**Why?** Customer name is FREQUENTLY accessed with order, but full customer data is not.

```json
{
  "_id": "order123",
  "customerId": "cust789",      // ← Reference (for full data when needed)
  "customerName": "John Doe"    // ← Subset (accessed together with order)
}
```

**Result:** 90% of queries need no lookup. 10% can still get full customer data.

#### ✅ Product Details STORED in OrderItem (Subset Pattern)
**Why?** Product name/price at order time is ALWAYS accessed with order items.

```json
{
  "items": [
    {
      "productId": "prod456",   // ← Reference (for current product data)
      "name": "Laptop Pro 15",  // ← Subset (historical snapshot)
      "price": 1299.99          // ← Subset (price at order time)
    }
  ]
}
```

**Result:** Order history queries are instant. Current product data still accessible.

---

## 💡 Why Embed OrderItems in MongoDB?

### The Power of Embedded Documents

In this demo, `OrderItem` objects are **embedded** directly within the `Order` document rather than being stored in a separate collection. This is a key MongoDB design pattern that exemplifies **"data accessed together, stored together"**.

### Benefits of Embedding:

1. **Single Query Performance**
   - Retrieve an order with all its items in ONE database query
   - No joins required (unlike relational databases)
   - Faster read performance for order details

2. **Atomic Operations**
   - The entire order (including all items) is updated atomically
   - No risk of partial updates or inconsistent state
   - Transactional integrity at the document level

3. **Data Locality**
   - Related data is stored together on disk
   - Better cache utilization
   - Reduced I/O operations

4. **Natural Data Model**
   - Matches how we think about orders in the real world
   - An order "contains" items - this is reflected in the data structure
   - JSON structure mirrors the domain model

### When to Embed vs. Reference?

**Embed when:**
- Data is accessed together (like orders and their items)
- One-to-few relationships (an order has a limited number of items)
- Data doesn't change independently (order items are fixed once ordered)

**Reference when:**
- Data is accessed independently
- One-to-many or many-to-many relationships with large cardinality
- Data changes frequently and independently

### Example: Why We Reference Products and Customers

Notice that:
- `Order` contains a `customerId` field that **references** the Customer collection
- `OrderItem` contains a `productId` field that **references** the Product collection

This is because:
- Customer information (email, phone) changes independently
- Product information (price, inventory) changes independently
- Customers and products are shared across many orders
- We want a single source of truth for customer and product data

This demonstrates a **hybrid approach**:
- **Embedding** order items for performance (accessed together)
- **Referencing** customers and products for data integrity (shared data)

### The Subset Pattern: Storing Customer ID and Name

Notice that `Order` stores **both** `customerId` (reference) and `customerName` (subset of customer data).

**This is the MongoDB "Subset Pattern" - a best practice for optimizing read performance:**

**What is the Subset Pattern?**
- Store a **reference** to the full document (customerId)
- Store a **subset** of frequently accessed fields (customerName)
- Keep the full document in its own collection for complete data

**Benefits:**
1. **Query Performance** - Display order lists without joining to customers collection
2. **Historical Accuracy** - Preserves customer name at time of order
3. **Reduced Complexity** - Common queries like "show all orders" don't need lookups
4. **Better UX** - Order reports can be generated instantly
5. **Flexibility** - Can still access full customer data when needed via customerId

**Trade-off:**
- Data duplication (customer name in both collections)
- If customer changes name, old orders keep the old name (usually desired for historical accuracy!)

**When to use the Subset Pattern:**
- Data is read frequently with the parent document
- Only a few fields from the referenced document are needed most of the time
- Read performance is more critical than write complexity
- You still need access to the full referenced document occasionally

**Pattern in this demo:**
- `customerId` → reference for data integrity and full customer lookups
- `customerName` → subset for display and performance
- Similarly, `OrderItem` uses the Subset Pattern with `productId` + `name`/`price` (snapshot at order time)

**Example Query Comparison:**

*Without Subset Pattern (requires lookup):*
```javascript
// Get orders
const orders = db.orders.find()
// For each order, lookup customer
orders.forEach(order => {
  const customer = db.customers.findOne({_id: order.customerId})
  console.log(customer.name)
})
```

*With Subset Pattern (no lookup needed):*
```javascript
// Get orders with customer name already included
db.orders.find().forEach(order => {
  console.log(order.customerName)  // Already available!
})
```

---

## 🎓 Key Concepts Demonstrated

### 1. Document-Oriented Storage
MongoDB stores data as JSON-like documents (BSON), making it natural to work with Java objects.

### 2. Spring Data MongoDB
- `@Document` annotation maps Java classes to MongoDB collections
- `@Id` annotation marks the primary key field
- `MongoRepository` provides built-in CRUD operations

### 3. Automatic Mapping
Spring Data MongoDB automatically converts:
- Java objects → BSON documents (when saving)
- BSON documents → Java objects (when retrieving)

### 4. Business Logic in Controllers
The `OrderController` demonstrates:
- Automatic `orderDate` assignment
- Total calculation from embedded items
- Clean separation of concerns

---

## 🔍 Viewing Data in MongoDB

```bash
# Connect to MongoDB shell
mongosh

# Switch to the database
use sample_pc_oms

# View all customers
db.customers.find().pretty()

# View all products
db.products.find().pretty()

# View all orders
db.orders.find().pretty()

# Count documents
db.customers.countDocuments()
db.products.countDocuments()
db.orders.countDocuments()

# Find orders by customer ID
db.orders.find({ "customerId": "65a1b2c3d4e5f6g7h8i9j0k0" }).pretty()
```

---

## 📚 Project Structure

```
com.example.store
├── model
│   ├── Customer.java          # Customer entity (@Document)
│   ├── Product.java           # Product entity (@Document)
│   ├── Order.java             # Order entity (@Document)
│   └── OrderItem.java         # Embedded document (no @Document)
├── repository
│   ├── CustomerRepository.java # Spring Data MongoDB repository
│   ├── ProductRepository.java  # Spring Data MongoDB repository
│   └── OrderRepository.java    # Spring Data MongoDB repository
├── controller
│   ├── CustomerController.java # REST endpoints for customers
│   ├── ProductController.java  # REST endpoints for products
│   └── OrderController.java    # REST endpoints for orders
└── DemoApplication.java        # Spring Boot main class
```

---

## 🎤 Webinar Talking Points

### Option A: Using the Web Interface (Recommended) 🌐

1. **Start with MongoDB basics** (2 min)
   - Show how MongoDB stores JSON-like documents
   - Compare to relational tables
   - Open the web interface at `http://localhost:8080`

2. **Show the data model** (3 min)
   - Walk through Product, Order, OrderItem classes
   - Highlight @Document vs. embedded documents
   - Explain the hybrid approach (embed items, reference products)

3. **Demonstrate MongoDB patterns interactively** (7 min)
   - **Create Customer** - Show basic document creation
   - **Create Electronics/Clothing/Book** - Demonstrate Polymorphic Pattern
   - **Get All Products** - Show different types in one collection
   - **Create Order** - Show Embedding, Subset, and Computed patterns
   - **Create Large Order** - Click "⚡ Generate 150 Items" to demo Outlier Pattern
   - **Get Order Items** - Show transparent bucket retrieval
   - Point out the pattern badges in the UI!

4. **Show the MongoDB documents** (2 min)
   - Open MongoDB Compass or shell
   - Show the actual documents created
   - Highlight embedded items, subset fields, and buckets

5. **Explain the patterns** (1 min)
   - Why embedding is powerful
   - When to use Outlier Pattern (100+ items)
   - Performance benefits

### Option B: Using curl Commands (Traditional)

1. **Start with MongoDB basics** (2 min)
   - Show how MongoDB stores JSON-like documents
   - Compare to relational tables

2. **Show the data model** (3 min)
   - Walk through Product, Order, OrderItem classes
   - Highlight @Document vs. embedded documents
   - Explain the hybrid approach (embed items, reference products)

3. **Live code a simple endpoint** (5 min)
   - Create ProductController.createProduct()
   - Show how @RestController and @PostMapping work
   - Demonstrate repository.save()

4. **Test the API** (3 min)
   - Use curl to create products
   - Create an order with embedded items
   - Show the MongoDB documents in the shell

5. **Explain embedded documents** (2 min)
   - Why embedding is powerful
   - When to embed vs. reference
   - Performance benefits

---

## 🚀 Next Steps for Attendees

After the webinar, attendees can extend this demo by:

1. Adding more endpoints (GET by ID, UPDATE, DELETE)
2. Implementing product search by category
3. Adding validation (e.g., check inventory before creating orders)
4. Implementing order status tracking
5. Adding pagination for large result sets
6. Creating custom queries with Spring Data MongoDB
7. Adding error handling and exception management
8. Implementing DTOs for better API design

---

## 📖 Resources

- [Spring Data MongoDB Documentation](https://spring.io/projects/spring-data-mongodb)
- [MongoDB Java Driver](https://www.mongodb.com/docs/drivers/java/)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [MongoDB Data Modeling Guide](https://www.mongodb.com/docs/manual/core/data-modeling-introduction/)

---

## 📝 License

This demo project is provided as-is for educational purposes.


