# Webinar Outline: Building Apps with Java, Spring Boot & MongoDB

**Duration:** 15 minutes
**Target Audience:** Java developers new to MongoDB

---

## Minute 0-2: Introduction & Setup

### What We'll Build
- Product Catalog + Order Management System
- REST API with Spring Boot
- MongoDB for data persistence
- **ACID Transactions** for data consistency 🆕
- **8 MongoDB Design Patterns** in action 🆕

### Key Concepts to Cover
- Document-oriented storage
- Embedded documents
- Natural Java-to-MongoDB mapping
- **MongoDB ACID transactions** 🆕
- **Inventory management with transactions** 🆕

### Show the Running App
```bash
# Terminal 1: Start MongoDB with replica set (required for transactions)
docker-compose up -d

# Terminal 2: Start Spring Boot
mvn spring-boot:run

# Terminal 3: Open web interface
open http://localhost:8080
```

---

## Minute 2-5: Data Model Explanation

### Show the Four Main Classes

1. **Product.java** (1.5 minutes)
   - `@Document(collection = "products")` - maps to MongoDB collection
   - `@Id` - MongoDB document ID
   - Simple POJO with Lombok annotations
   - Fields: id, name, price, category, **inventory**, sku, description
   - **Polymorphic Pattern**: Electronics/Clothing/Book types 🆕

2. **Customer.java** (0.5 minute)
   - `@Document(collection = "customers")`
   - Fields: id, name, email, phone
   - **Referenced** from Orders (foreign key relationship)

3. **OrderItem.java** (1 minute)
   - **No @Document annotation** - this is key!
   - Embedded within Order documents
   - Fields: productId, name, price, quantity
   - **Subset Pattern**: Stores product name/price for historical accuracy

4. **Order.java** (2 minutes)
   - `@Document(collection = "orders")`
   - Contains `List<OrderItem>` - embedded documents
   - Fields: id, **customerId**, customerName, orderDate, items, total
   - **Outlier Pattern**: Large orders (100+ items) use bucketing 🆕
   - **Schema Versioning**: Tracks schema evolution (v3) 🆕

### Key Teaching Points
> "Notice OrderItem has no @Document annotation. It's not a separate collection - it's embedded inside Order documents. This is MongoDB's superpower!"

> "We use the **Subset Pattern** - storing customerId + customerName in the order. We get the customer name for fast queries, and customerId for data consistency!"

> "For large orders (100+ items), we use the **Outlier Pattern** - items stored in separate bucket documents to avoid the 16MB document limit."

---

## Minute 5-8: Live Coding a Controller

### Create ProductController (or show existing)

```java
@RestController
@RequestMapping("/products")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }
}
```

### Talking Points
- `@RestController` - combines @Controller + @ResponseBody
- `@RequestMapping` - base path for all endpoints
- `MongoRepository` - Spring Data provides CRUD operations automatically
- `save()` - converts Java object to BSON and stores in MongoDB
- `findAll()` - retrieves all documents and converts to Java objects

---

## Minute 8-11: Demo the API (Use Web Interface!)

### **Option 1: Interactive Web Interface** 🆕 (Recommended for Webinars)
```bash
# Open the web interface
open http://localhost:8080
```

**Demo Flow:**
1. **Create a Customer** - Click "POST /customers" → "Send Request"
2. **Create a Product** - Click "POST /products" → "Send Request"
3. **Create an Order** - Click "POST /orders" → Paste customer/product IDs → "Send Request"
4. **Show Inventory Decrement** - Click "GET /products" → See inventory reduced!

**Highlight:**
- ✨ Beautiful, MongoDB-themed UI
- 📋 Pattern badges show which MongoDB patterns are used
- 🎯 One-click testing - perfect for webinars!

---

### **Option 2: curl Commands** (Traditional)

#### Create a Customer
```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "555-1234"
  }'
# Copy the customer ID from response
```

#### Create a Product with Inventory
```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50,
    "sku": "LAPTOP-001",
    "description": "High-performance laptop"
  }'
# Copy the product ID from response
```

#### Create an Order (With Transaction!)
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "PASTE_CUSTOMER_ID",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "PASTE_PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 2
      }
    ]
  }'
```

### Show in MongoDB Compass or Shell
```bash
mongosh
use product_catalog_oms

# See the customer
db.customers.find().pretty()

# See the product (inventory should be 48 now!)
db.products.find().pretty()

# See the order with embedded items
db.orders.find().pretty()
```

### Point Out 🆕
- ✅ **Transaction**: Order creation + inventory decrement happened atomically
- ✅ **Subset Pattern**: Order stores customerId + customerName
- ✅ **Embedding Pattern**: OrderItems embedded in Order
- ✅ **Referential Integrity**: customerId validated (must exist!)
- ✅ **Inventory Management**: Can't oversell - validation prevents it!

---

## Minute 11-13: MongoDB Design Patterns & Transactions

### Show the Order Document Structure 🆕
```json
{
  "_id": "...",
  "schemaVersion": 3,
  "customerId": "cust123",              // ← Reference to customer
  "customerName": "John Doe",           // ← Subset Pattern (denormalized)
  "orderDate": "2024-01-15T10:30:00",
  "isLargeOrder": false,
  "items": [                            // ← Embedded array
    {
      "productId": "prod456",           // ← Reference to product
      "name": "Laptop Pro 15",          // ← Subset Pattern
      "price": 1299.99,                 // ← Subset Pattern
      "quantity": 2
    }
  ],
  "total": 2599.98                      // ← Computed Pattern
}
```

### Eight MongoDB Patterns in Action! 🆕

1. **Embedding Pattern** - OrderItems embedded in Orders
2. **Reference Pattern** - customerId, productId links to other collections
3. **Subset Pattern** - customerName, product name/price denormalized for fast reads
4. **Computed Pattern** - Total pre-calculated and stored
5. **Polymorphic Pattern** - Different product types (Electronics/Clothing/Books)
6. **Document Versioning** - schemaVersion tracks schema evolution
7. **Outlier Pattern** - Large orders (100+ items) use bucketing
8. **Transaction Pattern** - Order + inventory updated atomically

### Why This Approach?
- ✅ **Embed items**: Always accessed with order (1 query, not 2)
- ✅ **Reference customer**: Shared across orders, changes independently
- ✅ **Denormalize name**: Fast queries, no joins needed
- ✅ **Store price**: Historical accuracy (even if product price changes later!)
- ✅ **Transactions**: Order + inventory always consistent

### MongoDB's Guiding Principle 🆕
> **"Data that is accessed together should be stored together."**

- Order + Items? **Always accessed together** → Embed!
- Customer details? **Occasionally accessed** → Reference + Subset!
- Product catalog? **Rarely accessed** → Reference only!

---

## Minute 13-14: Show Transaction Logic 🆕

### Highlight OrderTransactionService
```java
@Service
public class OrderTransactionService {

    @Transactional  // ← MongoDB ACID transaction!
    public Order createOrderWithInventoryUpdate(Order order) {
        // 1. Validate customer exists
        Customer customer = customerRepository.findById(order.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(order.getCustomerId()));

        // 2. Validate products & check inventory
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            if (product.getInventory() < item.getQuantity()) {
                throw new InsufficientInventoryException(...);
            }
        }

        // 3. Save order
        Order savedOrder = orderRepository.save(order);

        // 4. Update inventory
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).get();
            product.setInventory(product.getInventory() - item.getQuantity());
            productRepository.save(product);
        }

        return savedOrder;  // ← Transaction commits here!
    }
}
```

### Talking Points 🆕
- ✅ **@Transactional** - All-or-nothing guarantee
- ✅ **Customer validation** - Referential integrity enforcement
- ✅ **Product validation** - Can't order non-existent products
- ✅ **Inventory checking** - Prevents overselling
- ✅ **Atomic updates** - Order + inventory updated together
- ✅ **Automatic rollback** - Any failure rolls back everything

### Large Orders (100+ items) 🆕
- Uses **Outlier Pattern** with bucketing
- Same transaction guarantees!
- Items split into 50-item buckets to avoid 16MB limit
- All buckets + inventory updated in ONE transaction

---

## Minute 14-15: Wrap Up & Next Steps

### What We Covered 🆕
✅ MongoDB stores JSON-like documents
✅ Java objects map naturally to MongoDB
✅ Spring Boot REST API with interactive web interface
✅ **8 MongoDB design patterns** in action
✅ **ACID transactions** for data consistency
✅ **Inventory management** with validation
✅ **Referential integrity** (customer/product validation)
✅ **Outlier Pattern** for large orders (100+ items)
✅ Embedded documents for related data

### Key Takeaways
> **"Data accessed together, stored together"** - MongoDB's guiding principle

**Design Pattern Summary:**
1. **Embed** when data is accessed together (Order → Items)
2. **Reference** when data is shared or independent (Order → Customer)
3. **Denormalize** frequently accessed fields (customerName in Order)
4. **Use transactions** when consistency matters (Order + Inventory)
5. **Handle outliers** gracefully (bucketing for 100+ items)

### Next Steps for Attendees
1. ⭐ Clone the demo repo
2. 🌐 Use the interactive web interface at http://localhost:8080
3. 📚 Read the comprehensive guides (8 markdown files included!)
4. 🧪 Test transaction rollbacks (try ordering more than available)
5. 📊 Try the AI Order Assistant (natural language orders!)
6. 🔍 Explore MongoDB Hybrid Search with vector embeddings
7. 🎨 Study the 8 design patterns in the code

### Resources
- **Demo code**: [GitHub link]
- **Interactive docs**: Included in the repo (BEGINNERS_GUIDE.md, SCHEMA_PATTERNS_GUIDE.md, TRANSACTIONS_GUIDE.md, etc.)
- **Spring Data MongoDB docs**: https://spring.io/projects/spring-data-mongodb
- **MongoDB University**: Free courses at https://learn.mongodb.com
- **MongoDB Atlas**: Free cloud database at https://cloud.mongodb.com
- **MongoDB Design Patterns**: https://www.mongodb.com/blog/post/building-with-patterns-a-summary

### Q&A
Open for questions!

---

## Preparation Checklist 🆕

Before the webinar:
- [ ] MongoDB running **with replica set** (docker-compose up -d)
- [ ] Verify replica set initialized: `mongosh` → `rs.status()`
- [ ] Application builds successfully (mvn clean install)
- [ ] Application starts successfully (mvn spring-boot:run)
- [ ] **Web interface accessible** at http://localhost:8080
- [ ] Test all web interface buttons (recommended demo method!)
- [ ] Alternative: Test all curl commands if using CLI
- [ ] Have mongosh or MongoDB Compass ready
- [ ] Prepare IDE with key files open:
  - `OrderTransactionService.java` (transaction logic)
  - `Order.java` (design patterns)
  - `OrderCreatorService.java` (large order bucketing)
- [ ] Test screen sharing setup
- [ ] Load sample data (products + customers)
- [ ] Have backup customer/product IDs ready

## Terminal Setup 🆕

**Option 1: Web Interface (Recommended)**
1. **Browser**: http://localhost:8080 (main demo interface)
2. **Terminal 1**: Application logs (mvn spring-boot:run)
3. **Terminal 2**: MongoDB Compass or mongosh (show data)
4. **IDE**: Code editor with transaction logic visible

**Option 2: Traditional CLI**
1. **Terminal 1**: MongoDB shell (mongosh)
2. **Terminal 2**: Application logs (mvn spring-boot:run)
3. **Terminal 3**: curl commands
4. **IDE**: Code editor with files open

## Common Issues & Solutions 🆕

**Issue**: MongoDB connection refused
**Solution**: Ensure MongoDB is running: `docker ps`

**Issue**: Transaction errors ("not a replica set")
**Solution**: Verify replica set: `mongosh` → `rs.status()`. Reinitialize if needed.

**Issue**: Port 8080 already in use
**Solution**: Change port in application.properties or kill process on 8080

**Issue**: Customer/Product ID not found when creating order
**Solution**: Use the web interface to list customers/products first, copy real IDs

**Issue**: Insufficient inventory error
**Solution**: This is expected behavior! Show it as a feature (transaction rollback demo)

**Issue**: Web interface not loading
**Solution**: Check `src/main/resources/static/index.html` exists. Clear browser cache.

---

## Bonus Content (If Time Permits) 🆕

### 1. AI Order Assistant Demo 🤖
```bash
# Natural language order processing!
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "I want 2 laptops for John Doe"}'
```

**Highlights:**
- ✅ GPT-5.4 powered natural language understanding
- ✅ MongoDB Hybrid Search (vector + text search)
- ✅ Automatic customer resolution
- ✅ Intelligent product matching
- ✅ Inventory validation with helpful error messages

### 2. Large Order Demo (Outlier Pattern)
```bash
# Use the web interface "Generate Large Order" button
# Creates 150 items → 3 buckets automatically
```

**Show in MongoDB:**
```javascript
// Main order (items = null)
db.orders.find({ "isLargeOrder": true })

// Bucket documents
db.order_item_buckets.find({ "orderId": "ORDER_ID" })
```

### 3. Transaction Rollback Demo
```bash
# Try to order more than available inventory
# Watch the transaction rollback!
```

### 4. Show Repository Interface
```java
public interface ProductRepository extends MongoRepository<Product, String> {
    // Spring Data generates implementation automatically!
}
```

### 5. Custom Query Examples
```java
List<Product> findByCategory(String category);
List<Product> findByPriceLessThan(BigDecimal price);
List<Product> findByInventoryGreaterThan(int threshold);
```

### 6. Polymorphic Pattern Demo
```bash
# Show different product types in MongoDB
db.products.find({ "type": "Electronics" })  // Has warranty, brand
db.products.find({ "type": "Clothing" })     // Has size, color, material
db.products.find({ "type": "Book" })         // Has author, isbn, pages
```

### 7. MongoDB Hybrid Search Demo 🔍
```bash
# Semantic search - understands meaning, not just keywords
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "I need comfortable running shoes for Alice"}'

# Matches "Athletic Sneakers" even though query said "running shoes"!
```

