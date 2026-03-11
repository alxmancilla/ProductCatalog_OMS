# Webinar Outline: Building Apps with Java, Spring Boot & MongoDB

**Duration:** 15 minutes  
**Target Audience:** Java developers new to MongoDB

---

## Minute 0-2: Introduction & Setup

### What We'll Build
- Product Catalog + Simple Order Management System
- REST API with Spring Boot
- MongoDB for data persistence

### Key Concepts to Cover
- Document-oriented storage
- Embedded documents
- Natural Java-to-MongoDB mapping

### Show the Running App
```bash
# Terminal 1: Start MongoDB
docker-compose up -d

# Terminal 2: Start Spring Boot
mvn spring-boot:run
```

---

## Minute 2-5: Data Model Explanation

### Show the Three Classes

1. **Product.java** (2 minutes)
   - `@Document(collection = "products")` - maps to MongoDB collection
   - `@Id` - MongoDB document ID
   - Simple POJO with Lombok annotations
   - Fields: id, name, price, category, inventory

2. **OrderItem.java** (1 minute)
   - **No @Document annotation** - this is key!
   - Embedded within Order documents
   - Fields: productId, name, price, quantity

3. **Order.java** (2 minutes)
   - `@Document(collection = "orders")`
   - Contains `List<OrderItem>` - embedded documents
   - Fields: id, customer, orderDate, items, total

### Key Teaching Point
> "Notice OrderItem has no @Document annotation. It's not a separate collection - it's embedded inside Order documents. This is MongoDB's superpower!"

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

## Minute 8-11: Demo the API

### Create Products
```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50
  }'
```

### Show in MongoDB Shell
```bash
mongosh
use product_catalog_oms
db.products.find().pretty()
```

### Point Out
- JSON request → Java object → BSON document
- MongoDB auto-generates `_id` field
- `_class` field for polymorphic queries (Spring Data adds this)

### Create an Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "John Doe",
    "items": [
      {
        "productId": "...",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 1
      }
    ]
  }'
```

### Show Order in MongoDB
```bash
db.orders.find().pretty()
```

---

## Minute 11-13: Explain Embedded Documents

### Show the Order Document Structure
```json
{
  "_id": "...",
  "customer": "John Doe",
  "orderDate": "2024-01-15T10:30:00",
  "items": [                          // ← Embedded array
    {
      "productId": "...",
      "name": "Laptop Pro 15",
      "price": 1299.99,
      "quantity": 1
    }
  ],
  "total": 1299.99
}
```

### Why Embed?
1. **Single Query** - Get order + all items in one read
2. **Atomic Updates** - Update entire order atomically
3. **Data Locality** - Related data stored together
4. **Natural Model** - Matches real-world: "an order contains items"

### When to Embed vs. Reference?
- **Embed**: Data accessed together, one-to-few, doesn't change independently
- **Reference**: Data accessed separately, one-to-many, changes independently

### Hybrid Approach in This Demo
- OrderItems **embedded** in Orders (accessed together)
- Products **referenced** from OrderItems (shared, change independently)

---

## Minute 13-14: Show OrderController Logic

### Highlight Business Logic
```java
@PostMapping
public ResponseEntity<Order> createOrder(@RequestBody Order order) {
    // Auto-set order date
    order.setOrderDate(LocalDateTime.now());
    
    // Calculate total from items
    BigDecimal total = order.getItems().stream()
        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    order.setTotal(total);
    
    return new ResponseEntity<>(orderRepository.save(order), HttpStatus.CREATED);
}
```

### Talking Points
- Business logic in controller (for simplicity)
- Automatic field calculation
- Stream API for total calculation
- MongoDB stores the complete document atomically

---

## Minute 14-15: Wrap Up & Next Steps

### What We Covered
✅ MongoDB stores JSON-like documents  
✅ Java objects map naturally to MongoDB  
✅ Spring Boot REST API in minutes  
✅ Embedded documents for related data  
✅ Basic CRUD operations  

### Next Steps for Attendees
1. Clone the demo repo
2. Add more endpoints (GET by ID, UPDATE, DELETE)
3. Implement product search by category
4. Add inventory validation
5. Explore Spring Data MongoDB queries
6. Try aggregation pipelines

### Resources
- Demo code: [GitHub link]
- Spring Data MongoDB docs
- MongoDB University (free courses)
- MongoDB Atlas (free cloud database)

### Q&A
Open for questions!

---

## Preparation Checklist

Before the webinar:
- [ ] MongoDB running (docker-compose up)
- [ ] Application builds successfully (mvn clean install)
- [ ] Test all curl commands
- [ ] Have mongosh ready in a terminal
- [ ] Prepare IDE with code open
- [ ] Test screen sharing setup
- [ ] Have backup product IDs ready for order creation

## Terminal Setup

Recommended terminal layout:
1. **Terminal 1**: MongoDB shell (mongosh)
2. **Terminal 2**: Application logs (mvn spring-boot:run)
3. **Terminal 3**: curl commands
4. **IDE**: Code editor with files open

## Common Issues & Solutions

**Issue**: MongoDB connection refused  
**Solution**: Ensure MongoDB is running: `docker ps`

**Issue**: Port 8080 already in use  
**Solution**: Change port in application.properties or kill process on 8080

**Issue**: Product ID not found when creating order  
**Solution**: Copy actual product ID from previous response

---

## Bonus Content (If Time Permits)

### Show Repository Interface
```java
public interface ProductRepository extends MongoRepository<Product, String> {
    // Spring Data generates implementation automatically!
}
```

### Custom Query Example
```java
List<Product> findByCategory(String category);
List<Product> findByPriceLessThan(BigDecimal price);
```

### Aggregation Example
```java
@Aggregation("{ $group: { _id: '$category', count: { $sum: 1 } } }")
List<CategoryCount> countByCategory();
```

