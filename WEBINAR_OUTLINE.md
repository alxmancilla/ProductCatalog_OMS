# Webinar Outline: Building Apps with Java, Spring Boot & MongoDB

**Duration:** 15 minutes
**Target Audience:** Java developers new to MongoDB

---

## Minute 0-2: Introduction & Setup

### What We'll Build
- **Complete Order Management System (OMS)** with all P0 features 🆕
- Product Catalog with inventory management
- REST API with Spring Boot
- MongoDB for data persistence
- **ACID Transactions** for data consistency 🆕
- **8 MongoDB Design Patterns** in action 🆕
- **Interactive Web-Based API Tester** 🆕

### Key Concepts to Cover
- Document-oriented storage
- Embedded documents (Order Items + Status History)
- Natural Java-to-MongoDB mapping
- **MongoDB ACID transactions** 🆕
- **Complete order lifecycle management** 🆕
- **Inventory management with delta calculation** 🆕
- **Audit trails and status tracking** 🆕
- **Advanced search and filtering** 🆕

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
   - Contains `List<StatusChange>` - embedded status history 🆕
   - Fields: id, **customerId**, customerName, orderDate, items, total, **status**, **statusHistory** 🆕
   - **Outlier Pattern**: Large orders (100+ items) use bucketing 🆕
   - **Schema Versioning**: Tracks schema evolution (v3) 🆕
   - **Audit Trail**: Complete history of all status changes 🆕

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

**Complete OMS Demo Flow (6 minutes):**

1. **Create a Customer** - Click "Create Customer" → "Send Request"
2. **Create Products** - Click "Create Product" → "Send Request" (create 2-3 products)
3. **Create an Order** - Click "Create Order" → Paste IDs → "Send Request"
4. **Show Inventory Decrement** - Click "Get All Products" → See inventory reduced!
5. **Get Order by ID** 🆕 - Copy order ID → Click "Get Order by ID" → See complete order with status history
6. **Update Order Status** 🆕 - Click "Update Order Status" → Change to "CONFIRMED" → See status history grow!
7. **Search Orders** 🆕 - Click "Search by Status" → Find all CONFIRMED orders
8. **Modify Order** 🆕 - Click "Update Order Items" → Change quantities → See inventory delta calculation!
9. **Cancel Order** 🆕 - Click "Cancel Order" → Watch inventory fully restored!
10. **Search by Customer** 🆕 - Click "Search by Customer" → See customer's order history
11. **View Analytics** 🆕📊 - Click "Analytics" section → See revenue by status, top customers, popular products!

**Highlight:**
- ✨ Beautiful, MongoDB-themed UI
- 📋 Pattern badges show which MongoDB patterns are used
- 🎯 One-click testing - perfect for webinars!
- 🆕 **Complete order lifecycle**: Create → Confirm → Modify → Cancel
- 🆕 **Real-time inventory tracking**: Watch delta calculations
- 🆕 **Audit trails**: Every status change recorded with who, when, why
- 🆕 **Business Intelligence**: MongoDB Aggregation Framework for analytics! 📊

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
  "status": "CONFIRMED",                // ← Current order status 🆕
  "isLargeOrder": false,
  "items": [                            // ← Embedded array
    {
      "productId": "prod456",           // ← Reference to product
      "name": "Laptop Pro 15",          // ← Subset Pattern
      "price": 1299.99,                 // ← Subset Pattern
      "quantity": 2
    }
  ],
  "statusHistory": [                    // ← Complete audit trail 🆕
    {
      "fromStatus": "PENDING",
      "toStatus": "CONFIRMED",
      "changedAt": "2024-01-15T10:35:00",
      "changedBy": "admin@example.com",
      "reason": "Payment confirmed",
      "metadata": {
        "paymentId": "pay_12345",
        "paymentMethod": "credit_card"
      }
    }
  ],
  "total": 2599.98                      // ← Computed Pattern
}
```

### Eight MongoDB Patterns in Action! 🆕

1. **Embedding Pattern** - OrderItems embedded in Orders
2. **Reference Pattern** - customerId, productId links to other collections
3. **Subset Pattern** - customerName, product name/price denormalized for fast reads
4. **Computed Pattern** - Total pre-calculated and stored (also used in analytics!)
5. **Polymorphic Pattern** - Different product types (Electronics/Clothing/Books)
6. **Document Versioning** - schemaVersion tracks schema evolution
7. **Outlier Pattern** - Large orders (100+ items) use bucketing
8. **Transaction Pattern** - Order + inventory updated atomically

**🆕 BONUS: Aggregation Framework** - Analytics endpoints showcase MongoDB's powerful aggregation pipeline for business intelligence!

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

## BONUS: Complete Order Management System Demo 🆕

### All P0 Features Implemented

**Show the comprehensive OMS capabilities:**

#### 1. **Order Status Management** (`PUT /orders/{id}/status`)
```bash
# Update order status with complete audit trail
curl -X PUT http://localhost:8080/orders/ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "CONFIRMED",
    "changedBy": "admin@example.com",
    "reason": "Payment confirmed",
    "metadata": {
      "paymentId": "pay_12345",
      "paymentMethod": "credit_card"
    }
  }'
```

**Talking Points:**
- ✅ **Business rules enforced**: Can't skip from PENDING to DELIVERED
- ✅ **Audit trail**: Every change tracked with who, when, why
- ✅ **Embedded history**: statusHistory array grows with each change
- ✅ **Metadata support**: Attach context (payment IDs, notes, etc.)

---

#### 2. **Order Search & Filtering** (Multiple endpoints)
```bash
# Search by status
curl "http://localhost:8080/orders?status=CONFIRMED"

# Search by customer
curl "http://localhost:8080/orders/search/by-customer?customerId=CUST_ID&status=CONFIRMED"

# Search by date range
curl "http://localhost:8080/orders/search/by-date?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59"

# Search by price range
curl "http://localhost:8080/orders/search/by-total?minTotal=100&maxTotal=1000"
```

**Talking Points:**
- ✅ **MongoDB indexes**: All queries optimized (status + orderDate compound index)
- ✅ **Flexible filtering**: Combine customer + status, date ranges, price ranges
- ✅ **Sorted results**: Always newest first
- ✅ **No collection scans**: All queries use indexes (verify with explain())

---

#### 3. **Order Updates with Inventory Delta** (`PUT /orders/{id}/items`)
```bash
# Modify order items - smart inventory adjustment!
curl -X PUT http://localhost:8080/orders/ORDER_ID/items \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "PROD_ID",
        "name": "Laptop Pro 15",
        "price": 1599.99,
        "quantity": 3
      }
    ],
    "updatedBy": "customer@example.com",
    "reason": "Customer increased quantity"
  }'
```

**Talking Points:**
- ✅ **Delta calculation**: Compares old vs new items
  - Example: Laptop(2) → Laptop(3) = delta of -1 inventory
  - Example: Remove Mouse(3) = delta of +3 inventory
- ✅ **Smart item removal**: Set quantity to 0 to remove an item
  - Item automatically removed from order
  - Inventory restored for that item
  - Order total recalculated
- ✅ **Only PENDING orders**: Business rule prevents modifying shipped orders
- ✅ **Recalculates total**: Computed Pattern updates order total
- ✅ **ACID transaction**: All changes or none

---

#### 4. **Order Cancellation with Inventory Restoration** (`POST /orders/{id}/cancel`)
```bash
# Cancel order and restore ALL inventory atomically
curl -X POST http://localhost:8080/orders/ORDER_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "cancelledBy": "customer@example.com",
    "reason": "Customer requested cancellation",
    "metadata": {
      "refundId": "ref_12345",
      "refundAmount": 2599.98
    }
  }'
```

**Talking Points:**
- ✅ **Full inventory restoration**: Every item returned to stock
- ✅ **ACID transaction**: Either all restored or nothing (no partial cancellations)
- ✅ **Status update**: Changes to CANCELLED with audit trail
- ✅ **Business rules**: Only PENDING/CONFIRMED orders can be cancelled
- ✅ **Transaction rollback demo**: Try deleting a product then cancel order → fails gracefully!

---

### OMS Testing & Validation

**Show the comprehensive test suite:**
```bash
# Run complete automated test suite
./test-oms-p0-features.sh

# Verify MongoDB indexes
./verify-mongodb-indexes.sh
```

**7 Test Scenarios Covered:**
1. ✅ Order creation with inventory decrement
2. ✅ Get order by ID
3. ✅ Status updates with history tracking
4. ✅ Search and filtering (customer, status, combined)
5. ✅ Order updates with delta calculation
6. ✅ Cancellation with inventory restoration
7. ✅ Error handling (invalid operations rejected)

---

## Minute 14-15: Wrap Up & Next Steps

### What We Covered 🆕
✅ MongoDB stores JSON-like documents
✅ Java objects map naturally to MongoDB
✅ Spring Boot REST API with interactive web interface
✅ **8 MongoDB design patterns** in action
✅ **ACID transactions** for data consistency
✅ **Complete Order Management System** with all P0 features 🆕
✅ **Order lifecycle management**: Create → Confirm → Process → Ship → Deliver
✅ **Order status tracking** with complete audit trails 🆕
✅ **Advanced search & filtering** (status, customer, date, price) 🆕
✅ **Order modifications** with smart inventory delta calculation 🆕
✅ **Order cancellation** with atomic inventory restoration 🆕
✅ **Analytics & BI** with MongoDB Aggregation Framework 🆕📊
✅ **Inventory management** with validation
✅ **Referential integrity** (customer/product validation)
✅ **Outlier Pattern** for large orders (100+ items)
✅ Embedded documents for related data (items + status history)

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
3. 📚 Read the comprehensive guides (10+ markdown files included!) 🆕
   - `OMS_P0_IMPLEMENTATION_PLAN.md` - Complete OMS architecture 🆕
   - `OMS_P0_TESTING_GUIDE.md` - Testing all P0 features 🆕
   - `TRANSACTIONS_GUIDE.md` - MongoDB ACID transactions
   - `OUTLIER_PATTERN_GUIDE.md` - Large order handling
   - `SCHEMA_PATTERNS_GUIDE.md` - All 8 design patterns
4. 🧪 Run the automated test suite: `./test-oms-p0-features.sh` 🆕
5. 🔍 Verify MongoDB indexes: `./verify-mongodb-indexes.sh` 🆕
6. 📊 Try the AI Order Assistant (natural language orders!)
7. 🔍 Explore MongoDB Hybrid Search with vector embeddings
8. 🎨 Study the 8 design patterns in the code
9. 🚀 Test the complete order lifecycle (create → modify → cancel) 🆕
10. 📈 Explore advanced search features (date ranges, price filters) 🆕

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
- [ ] **Test new OMS features in web interface:** 🆕
  - [ ] Get Order by ID
  - [ ] Update Order Status
  - [ ] Search by Status
  - [ ] Search by Customer
  - [ ] Search by Date Range
  - [ ] Search by Total Range
  - [ ] Update Order Items
  - [ ] Cancel Order
- [ ] Alternative: Test all curl commands if using CLI
- [ ] Have mongosh or MongoDB Compass ready
- [ ] Prepare IDE with key files open:
  - `OrderTransactionService.java` (transaction logic)
  - `OrderCancellationService.java` (cancellation with inventory restore) 🆕
  - `OrderUpdateService.java` (delta calculation logic) 🆕
  - `Order.java` (design patterns + status history) 🆕
  - `OrderController.java` (all OMS endpoints) 🆕
  - `OrderCreatorService.java` (large order bucketing)
- [ ] Test screen sharing setup
- [ ] Load sample data (products + customers)
- [ ] Have backup customer/product IDs ready
- [ ] Run test suite to verify everything works: `./test-oms-p0-features.sh` 🆕
- [ ] Verify indexes created: `./verify-mongodb-indexes.sh` 🆕

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

### 1. Analytics & Business Intelligence with Aggregation Framework 🆕📊

**Show MongoDB's powerful aggregation capabilities (2-3 minutes):**

MongoDB's Aggregation Framework = **SQL GROUP BY + Joins + Transforms** in one pipeline!

#### Revenue by Order Status
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

**MongoDB Pipeline (vs SQL):**
```javascript
// MongoDB Aggregation
db.orders.aggregate([
  { $match: { status: { $ne: "CANCELLED" } } },  // ← WHERE clause
  { $group: {                                      // ← GROUP BY
      _id: "$status",
      totalRevenue: { $sum: "$total" },           // ← SUM()
      orderCount: { $sum: 1 }                     // ← COUNT(*)
  }},
  { $sort: { totalRevenue: -1 } }                 // ← ORDER BY
])

// SQL Equivalent
SELECT status,
       SUM(total) as totalRevenue,
       COUNT(*) as orderCount
FROM orders
WHERE status != 'CANCELLED'
GROUP BY status
ORDER BY totalRevenue DESC
```

#### Top Customers by Spending
```bash
curl http://localhost:8080/analytics/orders/top-customers?limit=5
```

**Use Case:** VIP customer identification, loyalty programs, customer segmentation

#### Popular Products (Demonstrates $unwind!)
```bash
curl http://localhost:8080/analytics/orders/popular-products?limit=10
```

**MongoDB Magic:**
```javascript
db.orders.aggregate([
  { $unwind: "$items" },                          // ← Flatten embedded array!
  { $addFields: {                                  // ← Calculated field
      itemRevenue: { $multiply: ["$items.price", "$items.quantity"] }
  }},
  { $group: {
      _id: "$items.productId",
      totalQuantitySold: { $sum: "$items.quantity" },
      totalRevenue: { $sum: "$itemRevenue" }
  }},
  { $sort: { totalQuantitySold: -1 } },
  { $limit: 10 }
])
```

**Key Insight:** `$unwind` is unique to document databases! It "explodes" embedded arrays into separate documents for aggregation. **No JOIN needed** because items are embedded!

#### Daily Revenue Trends (Time-Series!)
```bash
curl http://localhost:8080/analytics/orders/daily-revenue?days=30
```

**MongoDB Date Aggregation:**
```javascript
db.orders.aggregate([
  { $match: { orderDate: { $gte: startDate } } },
  { $addFields: {
      dateString: { $dateToString: {            // ← Date formatting!
          format: "%Y-%m-%d",
          date: "$orderDate"
      }}
  }},
  { $group: {
      _id: "$dateString",
      totalRevenue: { $sum: "$total" },
      orderCount: { $sum: 1 }
  }},
  { $sort: { _id: -1 } }
])
```

**Perfect For:**
- 📊 Executive dashboards
- 📈 Revenue forecasting
- 🎯 Customer segmentation
- 📦 Inventory planning
- 🔍 Product performance analysis

**MongoDB Aggregation Benefits:**
✅ Server-side processing (not application-side)
✅ Pipeline approach (composable stages)
✅ Works with embedded documents (no joins!)
✅ Can use indexes for performance
✅ Results can be materialized to collections with `$merge` or `$out`

---

### 2. Complete Order Lifecycle Demo 🆕
**Show the full OMS in action (3-4 minutes):**

```bash
# Step 1: Create order
ORDER_ID=$(curl -s -X POST http://localhost:8080/orders ...)

# Step 2: Confirm order (PENDING → CONFIRMED)
curl -X PUT http://localhost:8080/orders/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "CONFIRMED", "changedBy": "admin", "reason": "Payment confirmed"}'

# Step 3: Modify order (add more items)
curl -X PUT http://localhost:8080/orders/$ORDER_ID/items \
  -H "Content-Type: application/json" \
  -d '{"items": [...], "updatedBy": "customer", "reason": "Added items"}'

# Step 4: Search for customer's orders
curl "http://localhost:8080/orders/search/by-customer?customerId=$CUSTOMER_ID"

# Step 5: Cancel order (restore inventory)
curl -X POST http://localhost:8080/orders/$ORDER_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"cancelledBy": "customer", "reason": "Changed mind"}'

# Step 6: Verify inventory restored
curl http://localhost:8080/products/$PRODUCT_ID
```

**Highlights:**
- ✅ Complete lifecycle in one demo
- ✅ Show status history growing
- ✅ Watch inventory deltas
- ✅ Demonstrate transaction rollback

---

### 2. Order Status Management Deep Dive 🆕
**Show the complete audit trail:**

```javascript
// In MongoDB Compass or mongosh
db.orders.findOne({"_id": ObjectId("ORDER_ID")})

// Focus on statusHistory array - complete audit trail
{
  "statusHistory": [
    {
      "fromStatus": null,
      "toStatus": "PENDING",
      "changedAt": "2024-01-15T10:00:00",
      "changedBy": "system",
      "reason": "Order created"
    },
    {
      "fromStatus": "PENDING",
      "toStatus": "CONFIRMED",
      "changedAt": "2024-01-15T10:05:00",
      "changedBy": "admin@example.com",
      "reason": "Payment confirmed",
      "metadata": {
        "paymentId": "pay_12345",
        "paymentMethod": "credit_card"
      }
    }
  ]
}
```

**Highlight:**
- ✅ Embedding Pattern for audit trail
- ✅ Complete change history
- ✅ Who, when, why for every status change
- ✅ Custom metadata support

---

### 3. Inventory Delta Calculation Demo 🆕
**Show smart inventory management:**

```bash
# Before: Check product inventory
curl http://localhost:8080/products/$LAPTOP_ID
# inventory: 20

# Create order with 5 laptops
curl -X POST http://localhost:8080/orders ...
# inventory: 15 (decremented by 5)

# Update order: Change quantity from 5 to 8
curl -X PUT http://localhost:8080/orders/$ORDER_ID/items \
  -d '{"items": [{"productId": "...", "quantity": 8}], ...}'
# inventory: 12 (decremented by 3 more, delta = -3)

# Remove item: Set quantity to 0
curl -X PUT http://localhost:8080/orders/$ORDER_ID/items \
  -d '{"items": [{"productId": "...", "quantity": 0}], ...}'
# inventory: 20 (restored completely, item removed from order)

# Cancel order
curl -X POST http://localhost:8080/orders/$ORDER_ID/cancel ...
# inventory: 20 (restored to original, delta = +8)
```

**Highlight:**
- ✅ Smart delta calculation (not full replacement)
- ✅ Only changes what's needed
- ✅ ACID transaction guarantees
- ✅ Prevents overselling

---

### 4. AI Order Assistant Demo 🤖
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

### 5. Large Order Demo (Outlier Pattern)
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

### 6. Transaction Rollback Demo
```bash
# Try to order more than available inventory
# Watch the transaction rollback!
```

### 7. Show Repository Interface
```java
public interface ProductRepository extends MongoRepository<Product, String> {
    // Spring Data generates implementation automatically!
}
```

### 8. Custom Query Examples
```java
List<Product> findByCategory(String category);
List<Product> findByPriceLessThan(BigDecimal price);
List<Product> findByInventoryGreaterThan(int threshold);
```

### 9. Polymorphic Pattern Demo
```bash
# Show different product types in MongoDB
db.products.find({ "type": "Electronics" })  // Has warranty, brand
db.products.find({ "type": "Clothing" })     // Has size, color, material
db.products.find({ "type": "Book" })         // Has author, isbn, pages
```

### 10. MongoDB Hybrid Search Demo 🔍
```bash
# Semantic search - understands meaning, not just keywords
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "I need comfortable running shoes for Alice"}'

# Matches "Athletic Sneakers" even though query said "running shoes"!
```

