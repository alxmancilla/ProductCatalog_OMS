# 🔨 Build a MongoDB OMS from Scratch
## 2-Hour Hands-On Workshop for Developers

---

## 🎯 What You'll Build

A working **Order Management System** with MongoDB, demonstrating:
- ✅ Document modeling (embedding)
- ✅ CRUD operations
- ✅ Indexes for performance
- ✅ ACID transactions
- ✅ Aggregation pipelines

**By the end:** You'll have built a real MongoDB application!

---

## 📋 Prerequisites

- ☕ Java 21 installed
- 🔧 Maven or Gradle
- 🍃 MongoDB running (local or Atlas)
- 💻 Your favorite IDE (IntelliJ, VS Code, Eclipse)

**Time:** 2 hours (6 modules × 20 minutes)

---

## 📦 Module 1: Project Setup (15 minutes)

### Step 1: Create Spring Boot Project

**Using Spring Initializr (https://start.spring.io):**

```
Project: Maven
Language: Java
Spring Boot: 3.2.x
Group: com.mycompany
Artifact: oms-demo
Name: oms-demo
Package: com.mycompany.oms
Java: 21

Dependencies:
- Spring Web
- Spring Data MongoDB
- Lombok
```

Click **Generate** → Download → Extract

### Step 2: Configure MongoDB Connection

**File:** `src/main/resources/application.properties`

```properties
# MongoDB Connection
spring.data.mongodb.uri=mongodb://localhost:27017/oms_workshop
spring.data.mongodb.database=oms_workshop

# JSON Configuration
spring.jackson.default-property-inclusion=non_null

# Logging
logging.level.org.springframework.data.mongodb=DEBUG
```

### Step 3: Verify Setup

```bash
cd oms-demo
mvn clean install
mvn spring-boot:run
```

**Expected:** Application starts on port 8080

✅ **Checkpoint:** Application runs successfully!

---

## 📄 Module 2: First Document Model (20 minutes)

### Step 1: Create Product Model

**File:** `src/main/java/com/mycompany/oms/model/Product.java`

```java
package com.mycompany.oms.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    
    private String name;
    private String sku;
    private BigDecimal price;
    private String category;
    private Integer inventory;
    
    // Metadata
    private String description;
}
```

**Key Concepts:**
- `@Document` - Maps to MongoDB collection
- `@Id` - MongoDB's `_id` field (auto-generated)
- `BigDecimal` - For money (precise decimals)

### Step 2: Create Repository

**File:** `src/main/java/com/mycompany/oms/repository/ProductRepository.java`

```java
package com.mycompany.oms.repository;

import com.mycompany.oms.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    
    // Spring Data generates these queries automatically!
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(String category);
}
```

**Magic:** Spring Data generates implementations!

### Step 3: Create REST Controller

**File:** `src/main/java/com/mycompany/oms/controller/ProductController.java`

```java
package com.mycompany.oms.controller;

import com.mycompany.oms.model.Product;
import com.mycompany.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductRepository productRepository;
    
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        return productRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### Step 4: Test It!

```bash
# Start application
mvn spring-boot:run

# Create a product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro",
    "sku": "LAPTOP-001",
    "price": 2499.00,
    "category": "Electronics",
    "inventory": 10,
    "description": "16-inch M3 Max"
  }'

# Get all products
curl http://localhost:8080/products
```

**Expected:** Your product returned with MongoDB-generated `id`!

✅ **Checkpoint:** You just created your first MongoDB document!

---

## 🎯 Module 3: Embedding Pattern (25 minutes)

### The Challenge

**SQL Way:**
```
orders table (id, customer, date)
  ↓ JOIN
order_items table (id, order_id, product, qty)
```
**Problem:** 2 queries + JOIN = slow!

**MongoDB Way:**
```json
{
  "customer": "John",
  "date": "2024-01-15",
  "items": [
    { "product": "Laptop", "qty": 1 }
  ]
}
```
**Benefit:** 1 query = fast!

### Step 1: Create Embedded OrderItem

**File:** `src/main/java/com/mycompany/oms/model/OrderItem.java`

```java
package com.mycompany.oms.model;

import lombok.Data;
import java.math.BigDecimal;

// NOT a @Document - this is embedded!
@Data
public class OrderItem {
    
    private String productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    
    // Computed field
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

**Note:** No `@Document` annotation - it's embedded, not a separate collection!

### Step 2: Create Order Model

**File:** `src/main/java/com/mycompany/oms/model/Order.java`

```java
package com.mycompany.oms.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;
    
    private String customerId;
    private String customerName;  // Subset Pattern!
    
    private List<OrderItem> items;  // EMBEDDING!
    
    private BigDecimal total;
    private String status;
    private LocalDateTime orderDate;
    
    // Computed Pattern - calculate total
    public void calculateTotal() {
        this.total = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**Key Points:**
- `List<OrderItem> items` - **Embedded array**
- `customerName` - **Subset Pattern** (frequently needed)
- `customerId` - **Reference** (rare full lookup)

### Step 3: Create Order Controller

**File:** `src/main/java/com/mycompany/oms/controller/OrderController.java`

```java
package com.mycompany.oms.controller;

import com.mycompany.oms.model.Order;
import com.mycompany.oms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderRepository orderRepository;
    
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        // Set metadata
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        
        // Calculate total (Computed Pattern)
        order.calculateTotal();
        
        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @GetMapping
    public List<Order> getAll() {
        return orderRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable String id) {
        return orderRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### Step 4: Create Repository

**File:** `src/main/java/com/mycompany/oms/repository/OrderRepository.java`

```java
package com.mycompany.oms.repository;

import com.mycompany.oms.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(String status);
}
```

### Step 5: Test Embedding!

```bash
# Create an order with EMBEDDED items
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust123",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "prod1",
        "productName": "MacBook Pro",
        "price": 2499.00,
        "quantity": 1
      },
      {
        "productId": "prod2",
        "productName": "Magic Mouse",
        "price": 79.00,
        "quantity": 2
      }
    ]
  }'

# Get the order (1 query gets EVERYTHING!)
curl http://localhost:8080/orders/{id}
```

**Result:**
```json
{
  "id": "...",
  "customerName": "John Doe",
  "items": [
    { "productName": "MacBook Pro", "quantity": 1, "subtotal": 2499.00 },
    { "productName": "Magic Mouse", "quantity": 2, "subtotal": 158.00 }
  ],
  "total": 2657.00,
  "status": "PENDING"
}
```

**✨ ONE query got the order AND all items!**

✅ **Checkpoint:** You understand embedding!

---

## 🏃 Module 4: Performance with Indexes (20 minutes)

### The Problem

**Without index:**
```
Find product by SKU → Scans ALL documents (slow!)
1,000 products = 450ms
10,000 products = 4,500ms
```

**With index:**
```
Find product by SKU → Uses index (fast!)
1,000 products = 2ms
10,000 products = 2ms
```

**225x faster!**

### Step 1: Create Index Configuration

**File:** `src/main/java/com/mycompany/oms/config/MongoIndexConfig.java`

```java
package com.mycompany.oms.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    CommandLineRunner createIndexes() {
        return args -> {
            log.info("Creating MongoDB indexes...");

            // Products indexes
            MongoCollection<Document> products =
                mongoTemplate.getCollection("products");

            // Unique index on SKU
            products.createIndex(
                Indexes.ascending("sku"),
                new IndexOptions().unique(true)
            );

            // Index on category (for filtering)
            products.createIndex(Indexes.ascending("category"));

            // Orders indexes
            MongoCollection<Document> orders =
                mongoTemplate.getCollection("orders");

            // Index on customerId
            orders.createIndex(Indexes.ascending("customerId"));

            // Index on status
            orders.createIndex(Indexes.ascending("status"));

            // Compound index (customerId + status)
            orders.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("customerId"),
                    Indexes.ascending("status")
                )
            );

            log.info("Indexes created successfully!");
        };
    }
}
```

### Step 2: Test Performance

```bash
# Create 1000 products (bulk)
for i in {1..1000}; do
  curl -X POST http://localhost:8080/products \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Product $i\",\"sku\":\"SKU-$i\",\"price\":99.99}" &
done
wait

# Query WITHOUT index (slow)
# First, drop the index temporarily
mongosh
> use oms_workshop
> db.products.dropIndex("sku_1")

# Time the query
time curl "http://localhost:8080/products?sku=SKU-500"
# Result: ~450ms (table scan)

# Recreate index
> db.products.createIndex({sku: 1}, {unique: true})

# Time again
time curl "http://localhost:8080/products?sku=SKU-500"
# Result: ~2ms (index seek)
```

**225x faster with index!**

✅ **Checkpoint:** You see the power of indexes!

---

## 💰 Module 5: ACID Transactions (30 minutes)

### The Challenge

**Problem:** Creating an order should:
1. Create the order
2. Decrement product inventory

**What if step 2 fails?** Order created but inventory not updated! ❌

**Solution:** ACID Transactions (all or nothing)!

### Step 1: Enable Transactions

**Requirement:** MongoDB must run as replica set!

```bash
# Check if replica set is initialized
mongosh
> rs.status()

# If not initialized:
> rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]})
```

### Step 2: Configure Transaction Manager

**File:** `src/main/java/com/mycompany/oms/config/MongoConfig.java`

```java
package com.mycompany.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
```

### Step 3: Create Transaction Service

**File:** `src/main/java/com/mycompany/oms/service/OrderService.java`

```java
package com.mycompany.oms.service;

import com.mycompany.oms.model.Order;
import com.mycompany.oms.model.OrderItem;
import com.mycompany.oms.model.Product;
import com.mycompany.oms.repository.OrderRepository;
import com.mycompany.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional  // ACID magic!
    public Order createOrderWithInventory(Order order) {
        // Step 1: Validate products exist
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new RuntimeException(
                    "Product not found: " + item.getProductId()
                ));

            // Step 2: Check inventory
            if (product.getInventory() < item.getQuantity()) {
                throw new RuntimeException(
                    "Insufficient inventory for: " + product.getName()
                );
            }

            // Step 3: Decrement inventory
            product.setInventory(product.getInventory() - item.getQuantity());
            productRepository.save(product);
        }

        // Step 4: Create order
        order.setStatus("CONFIRMED");
        order.calculateTotal();
        Order saved = orderRepository.save(order);

        // If ANY step fails → ALL changes are rolled back!
        return saved;
    }
}
```

### Step 4: Update Controller

```java
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;  // Use service instead of repository

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        try {
            Order saved = orderService.createOrderWithInventory(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

### Step 5: Test Transactions!

**Success Case:**
```bash
# Create product with inventory
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "sku": "PHONE-001",
    "price": 999.00,
    "inventory": 5
  }'

# Create order (should succeed)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust123",
    "customerName": "Jane Doe",
    "items": [{
      "productId": "<PRODUCT_ID>",
      "productName": "iPhone 15",
      "price": 999.00,
      "quantity": 2
    }]
  }'

# Check inventory (should be 3 now)
curl http://localhost:8080/products/<PRODUCT_ID>
# inventory: 3 ✅
```

**Failure Case (Insufficient Inventory):**
```bash
# Try to order 10 (only 3 left)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust123",
    "customerName": "Jane Doe",
    "items": [{
      "productId": "<PRODUCT_ID>",
      "productName": "iPhone 15",
      "price": 999.00,
      "quantity": 10
    }]
  }'

# Result: 400 Bad Request ❌
# Check inventory (should still be 3)
curl http://localhost:8080/products/<PRODUCT_ID>
# inventory: 3 ✅ (NOT decremented because transaction rolled back!)
```

**✨ Transaction ensures: All or nothing!**

✅ **Checkpoint:** You implemented ACID transactions!

---

## 📊 Module 6: Aggregation Pipeline (30 minutes)

### The Challenge

**Business Question:** "What's our total revenue by product category?"

**SQL Way:**
```sql
SELECT category, SUM(total)
FROM orders o
JOIN order_items i ON o.id = i.order_id
JOIN products p ON i.product_id = p.id
GROUP BY p.category
```

**MongoDB Way:** Aggregation Pipeline!

### Step 1: Create Analytics Service

**File:** `src/main/java/com/mycompany/oms/service/AnalyticsService.java`

```java
package com.mycompany.oms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MongoTemplate mongoTemplate;

    public List<RevenueByStatus> getRevenueByStatus() {
        Aggregation aggregation = newAggregation(
            // Stage 1: Group by status
            group("status")
                .sum("total").as("totalRevenue")
                .count().as("orderCount"),

            // Stage 2: Sort by revenue descending
            sort(org.springframework.data.domain.Sort.Direction.DESC, "totalRevenue")
        );

        AggregationResults<RevenueByStatus> results =
            mongoTemplate.aggregate(aggregation, "orders", RevenueByStatus.class);

        return results.getMappedResults();
    }

    // Result DTO
    public static class RevenueByStatus {
        public String _id;  // status
        public BigDecimal totalRevenue;
        public Long orderCount;

        // Getters/setters
    }
}
```

### Step 2: Create Analytics Controller

**File:** `src/main/java/com/mycompany/oms/controller/AnalyticsController.java`

```java
package com.mycompany.oms.controller;

import com.mycompany.oms.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue-by-status")
    public List<AnalyticsService.RevenueByStatus> getRevenueByStatus() {
        return analyticsService.getRevenueByStatus();
    }
}
```

### Step 3: Test Aggregation!

```bash
# Create some orders with different statuses
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Alice","items":[{"productName":"Laptop","price":1299,"quantity":1}]}'

# Update some to DELIVERED
curl -X PATCH http://localhost:8080/orders/{id} \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}'

# Run analytics
curl http://localhost:8080/analytics/revenue-by-status
```

**Result:**
```json
[
  {
    "_id": "DELIVERED",
    "totalRevenue": 5432.00,
    "orderCount": 15
  },
  {
    "_id": "PENDING",
    "totalRevenue": 2134.50,
    "orderCount": 8
  }
]
```

✅ **Checkpoint:** You built an analytics pipeline!

---

## 🎉 Workshop Complete!

### What You Built

✅ **Product Catalog** with CRUD operations
✅ **Order System** with embedded items (Embedding Pattern)
✅ **Subset Pattern** (customer name in orders)
✅ **Indexes** for 225x faster queries
✅ **ACID Transactions** for inventory management
✅ **Aggregation Pipeline** for analytics

### What You Learned

**MongoDB Patterns:**
- Embedding (items in orders)
- Subset (customer name duplication)
- Reference (customer ID link)
- Computed (total calculation)

**MongoDB Features:**
- Document modeling
- Spring Data MongoDB
- Indexes (unique, compound)
- Transactions (@Transactional)
- Aggregation Framework

**Performance:**
- 1 query vs N+1 queries
- Index impact (225x faster!)
- Embedding benefits

---

## 🚀 Next Steps

### Level Up Your Skills

**1. Add More Features:**
- Order cancellation (restore inventory)
- Order status updates (audit trail)
- Customer management
- Product search

**2. Learn Advanced Patterns:**
- **Outlier Pattern:** [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md)
- **CQRS:** [CQRS_IMPLEMENTATION_SUMMARY.md](CQRS_IMPLEMENTATION_SUMMARY.md)
- **Document Versioning:** [PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md)

**3. Practice Challenges:**
- **[EXERCISES.md](EXERCISES.md)** - 10 hands-on challenges
- **[ANTIPATTERNS.md](ANTIPATTERNS.md)** - Learn what NOT to do

**4. See Production Patterns:**
- This demo app has ALL features you just built!
- Plus CQRS, monitoring, security, testing
- Study the code: `src/main/java/com/example/store/`

---

## 📚 Reference Code

Your workshop code vs this demo:

| Feature | Your Code | Demo Code |
|---------|-----------|-----------|
| **Product Model** | ✅ Basic | ✅ + Polymorphic (Electronics/Clothing/Books) |
| **Order Model** | ✅ Basic | ✅ + Status history, versioning |
| **Indexes** | ✅ Basic | ✅ + Programmatic creation |
| **Transactions** | ✅ Basic | ✅ + Atomic inventory updates |
| **Aggregation** | ✅ 1 pipeline | ✅ + 4 pipelines + CQRS |

---

## 🎯 Key Takeaways

1. **Embedding is powerful** - 1 query > N queries
2. **Indexes matter** - 225x performance gain
3. **Transactions work** - ACID in MongoDB!
4. **Aggregation is flexible** - Complex analytics easily
5. **Spring Data is easy** - Auto-generated queries

---

**Time invested:** 2 hours
**Skills gained:** MongoDB application development
**Next step:** Build your own app or explore this demo's advanced features!

**Happy coding!** 🚀
