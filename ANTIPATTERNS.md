# ⚠️ MongoDB Anti-Patterns
## What NOT to Do - Learn from Common Mistakes!

---

## 🎯 Purpose

This guide shows **common MongoDB mistakes** and how to fix them. Learning what NOT to do is just as important as learning what to do!

**Format:**
- ❌ **Bad Example** - The mistake
- 🔴 **Problem** - Why it's bad
- ✅ **Good Example** - The fix
- 📊 **Impact** - Performance/reliability improvement

---

## ❌ Anti-Pattern 1: Massive Unbounded Arrays

### Bad Example

```json
{
  "_id": "user123",
  "name": "John Doe",
  "orders": [
    // 10,000 order objects embedded here!
    { "id": "order1", "total": 99.99, ... },
    { "id": "order2", "total": 149.99", ... },
    // ... 9,998 more
  ]
}
```

### 🔴 Problems

1. **16MB Document Limit** - Will crash when orders array gets too large
2. **Read Performance** - Loading user loads ALL orders (slow!)
3. **Write Performance** - Updating one order requires rewriting entire document
4. **Memory** - Cannot load user into memory
5. **Indexing** - Cannot index array elements efficiently

**Real Impact:**
- Document size: 15.8MB (approaching 16MB limit!)
- Read time: 2,500ms (vs 3ms for small doc)
- Update time: 1,200ms (vs 10ms)
- **Risk:** Application crash when limit exceeded

---

### ✅ Good Example (Outlier Pattern)

**Hybrid Approach:**
```json
// User document (always small)
{
  "_id": "user123",
  "name": "John Doe",
  "orderCount": 10000,
  "recentOrders": [
    // Last 10 orders embedded
    { "id": "order9999", "total": 99.99 },
    { "id": "order10000", "total": 149.99 }
  ]
}

// Separate orders collection
// orders: { userId: "user123", id: "order1", ... }
```

**Benefits:**
- ✅ User document: 2KB (vs 15.8MB)
- ✅ Read time: 3ms (vs 2,500ms) - **833x faster**
- ✅ Update: 10ms (vs 1,200ms) - **120x faster**
- ✅ No 16MB limit risk
- ✅ Can query orders independently

**See:** [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for complete guide

---

## ❌ Anti-Pattern 2: Unnecessary References (SQL Thinking)

### Bad Example

```json
// Order document
{
  "_id": "order123",
  "customerId": "cust456",
  "itemIds": ["item1", "item2", "item3"]  // Just IDs!
}

// Separate items collection
{
  "_id": "item1",
  "orderId": "order123",
  "name": "Laptop",
  "price": 1299.99
}
```

### 🔴 Problems

1. **N+1 Query Problem** - Same as SQL!
2. **Performance** - One query for order + N queries for items
3. **Complexity** - Application must join data
4. **Network** - Multiple round trips to database

**Real Impact:**
- Queries: 1 + 3 = 4 queries
- Time: 3ms + (3 × 3ms) = 12ms
- **4x slower than embedding**

---

### ✅ Good Example (Embed!)

```json
{
  "_id": "order123",
  "customerId": "cust456",
  "items": [  // Embed the actual items!
    {
      "name": "Laptop",
      "price": 1299.99,
      "quantity": 1
    },
    {
      "name": "Mouse",
      "price": 29.99,
      "quantity": 2
    }
  ]
}
```

**Benefits:**
- ✅ Queries: 1 (vs 4) - **4x fewer**
- ✅ Time: 3ms (vs 12ms) - **4x faster**
- ✅ Simple: No application joins
- ✅ Atomic: Update order and items together

**Rule:** If data is ALWAYS accessed together → EMBED IT!

---

## ❌ Anti-Pattern 3: No Indexes on Queries

### Bad Example

```javascript
// Frequent query but no index!
db.products.find({ sku: "LAPTOP-001" })

// MongoDB scans ALL documents (COLLSCAN)
```

### 🔴 Problems

1. **Table Scan** - Examines every document
2. **Linear Performance** - O(n) complexity
3. **CPU Waste** - 99.9% of work is unnecessary
4. **Slow** - Gets exponentially worse with scale

**Real Impact:**
- 1,000 products: 450ms
- 10,000 products: 4,500ms
- 100,000 products: 45,000ms
- **Unacceptable for production!**

---

### ✅ Good Example (Create Index!)

```javascript
// Create index on frequently queried field
db.products.createIndex({ sku: 1 }, { unique: true })

// Now query uses index (IXSCAN)
db.products.find({ sku: "LAPTOP-001" })
```

**Benefits:**
- ✅ Index scan: O(log n) complexity
- ✅ Time: 2ms (constant regardless of collection size!)
- ✅ Efficiency: 100% (examines only matching docs)
- ✅ Scalable: 1,000 or 1,000,000 docs = same speed

**Impact:** **225x faster** (450ms → 2ms)

**Rule:** Index every field you query on!

**This Demo:**
See `src/main/java/com/example/store/config/*IndexConfiguration.java` for programmatic index creation.

---

## ❌ Anti-Pattern 4: Storing Computed Values Without Recalculating

### Bad Example

```json
{
  "_id": "order123",
  "items": [
    { "price": 99.99, "quantity": 2 },
    { "price": 49.99, "quantity": 1 }
  ],
  "total": 249.97  // Stored but never updated!
}
```

**What if item price changes?**
```javascript
// Update item price
db.orders.updateOne(
  { "_id": "order123", "items.name": "Widget" },
  { $set: { "items.$.price": 79.99 }}
)

// Total is now WRONG! (249.97 vs actual 209.97)
```

### 🔴 Problems

1. **Data Inconsistency** - Total doesn't match items
2. **Silent Errors** - No validation
3. **Trust Issues** - Cannot trust computed fields

---

### ✅ Good Example (Recalculate or Use Aggregation)

**Option 1: Recalculate on Write**
```java
public void updateOrderItems(Order order, List<OrderItem> newItems) {
    order.setItems(newItems);
    order.calculateTotal();  // Recompute!
    repository.save(order);
}
```

**Option 2: Compute on Read (If Needed)**
```javascript
db.orders.aggregate([
  { $addFields: {
      computedTotal: {
        $sum: {
          $map: {
            input: "$items",
            as: "item",
            in: { $multiply: ["$$item.price", "$$item.quantity"] }
          }
        }
      }
  }}
])
```

**Benefits:**
- ✅ Always accurate
- ✅ No stale data
- ✅ Trustworthy

**Rule:** If you store computed values, ALWAYS recompute when source data changes!

---

## ❌ Anti-Pattern 5: Using _id as Business Key

### Bad Example

```java
// Use MongoDB _id as SKU
Product product = new Product();
product.setId("LAPTOP-001");  // Setting _id manually!
product.setName("MacBook Pro");
```

### 🔴 Problems

1. **Type Mismatch** - _id is ObjectId by default, you're using String
2. **Conflicts** - Hard to ensure uniqueness across distributed systems
3. **Inflexibility** - Cannot change business keys
4. **Best Practice Violation** - _id should be database-generated

**Real Issue:**
```javascript
// This fails if "LAPTOP-001" already exists
db.products.insertOne({ _id: "LAPTOP-001", name: "..." })
// DuplicateKeyError!
```

---

### ✅ Good Example (Separate Business Key)

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;  // MongoDB ObjectId (auto-generated)
    
    @Indexed(unique = true)
    private String sku;  // Business key (unique index)
    
    private String name;
}
```

**Benefits:**
- ✅ _id managed by MongoDB (guaranteed unique)
- ✅ SKU is indexed and unique (fast lookups)
- ✅ Can change SKU if needed (update field, not _id)
- ✅ Follows MongoDB best practices

**Rule:** Let MongoDB generate _id, use indexed fields for business keys!

---

## ❌ Anti-Pattern 6: Deeply Nested Documents (5+ Levels)

### Bad Example

```json
{
  "_id": "order123",
  "customer": {
    "name": "John",
    "address": {
      "street": {
        "number": {
          "building": {
            "apartment": {
              "room": "5A"  // 6 levels deep!
            }
          }
        }
      }
    }
  }
}
```

### 🔴 Problems

1. **Query Complexity** - Hard to query deeply nested fields
2. **Update Complexity** - Update paths are long and error-prone
3. **Indexing** - Cannot efficiently index deep fields
4. **Readability** - Hard to understand and maintain

**Real Impact:**
```javascript
// Update query is ugly!
db.orders.updateOne(
  { _id: "order123" },
  { $set: { "customer.address.street.number.building.apartment.room": "5B" }}
)

// Index name is ridiculous
db.orders.createIndex({ "customer.address.street.number.building.apartment.room": 1 })
```

---

### ✅ Good Example (Flatten Structure)

```json
{
  "_id": "order123",
  "customerId": "cust456",
  "customerName": "John",
  "address": {
    "street": "123 Main St",
    "apt": "5A",
    "city": "SF"
  }
}
```

**Benefits:**
- ✅ Simple queries: `address.apt`
- ✅ Easy updates
- ✅ Can index: `{ "address.apt": 1 }`
- ✅ Readable!

**Rule:** Keep nesting to 3 levels max. Flatten when possible!

---

## ❌ Anti-Pattern 7: Not Using Transactions for Multi-Doc Updates

### Bad Example

```java
public Order createOrder(Order order) {
    // Step 1: Save order
    Order saved = orderRepository.save(order);
    
    // Step 2: Update inventory
    for (OrderItem item : order.getItems()) {
        Product product = productRepository.findById(item.getProductId()).get();
        product.setInventory(product.getInventory() - item.getQuantity());
        productRepository.save(product);
    }
    
    return saved;
}
```

### 🔴 Problems

**What if step 2 fails?**
- ✅ Order created
- ❌ Inventory NOT updated
- 🔴 **Data Inconsistency!**

**Scenario:**
1. Customer orders 5 laptops
2. Order saved successfully
3. Network failure before inventory update
4. **Result:** 5 laptops sold but inventory unchanged!
5. **Impact:** Overselling, angry customers!

---

### ✅ Good Example (Use Transactions!)

```java
@Transactional  // All or nothing!
public Order createOrder(Order order) {
    // Step 1: Save order
    Order saved = orderRepository.save(order);
    
    // Step 2: Update inventory
    for (OrderItem item : order.getItems()) {
        Product product = productRepository.findById(item.getProductId()).get();
        
        if (product.getInventory() < item.getQuantity()) {
            throw new InsufficientInventoryException();
            // Transaction rolls back - order NOT created!
        }
        
        product.setInventory(product.getInventory() - item.getQuantity());
        productRepository.save(product);
    }
    
    return saved;
    // If we reach here, BOTH order and inventory are committed together!
}
```

**Benefits:**
- ✅ Atomicity: All or nothing
- ✅ Consistency: Data always valid
- ✅ Reliability: Automatic rollback on failure
- ✅ No overselling!

**Rule:** Use transactions for multi-document updates that must be atomic!

**Overhead:** +40% write time, but worth it for consistency!

---

## ❌ Anti-Pattern 8: Ignoring Write Concerns

### Bad Example

```java
// Using default write concern (w:1)
mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);

// Write succeeds on primary
orderRepository.save(order);

// Primary crashes before replicating
// Data lost! ❌
```

### 🔴 Problems

1. **Data Loss Risk** - If primary fails before replication
2. **No Guarantees** - Write may not be durable
3. **Production Risk** - Unacceptable for critical data

**Scenario:**
1. Write order to primary (w:1)
2. Primary acknowledges
3. Primary crashes
4. Data not replicated to secondaries
5. **Order lost!**

---

### ✅ Good Example (Use w:majority)

```java
// This demo uses w:majority
@Bean
public MongoClientSettingsBuilderCustomizer mongoClientSettings() {
    return builder -> builder
        .writeConcern(WriteConcern.MAJORITY.withJournal(true));
}
```

**Benefits:**
- ✅ Survives node failures
- ✅ Guaranteed durable
- ✅ Production-safe!

**Trade-off:** +10-20ms write latency (acceptable for safety!)

**Rule:** Use `w:majority` for production writes!

---

## ❌ Anti-Pattern 9: Not Paginating Large Results

### Bad Example

```java
@GetMapping("/orders")
public List<Order> getAllOrders() {
    return orderRepository.findAll();  // Returns ALL orders!
}
```

### 🔴 Problems

**With 5,000 orders:**
- Memory: 50MB
- Network: 50MB download
- Time: 3,500ms
- **Browser crashes!** ❌

**With 50,000 orders:**
- Memory: 500MB
- Network: 500MB download
- Time: 35,000ms
- **OutOfMemoryError!** ❌

---

### ✅ Good Example (Paginate!)

```java
@GetMapping("/orders")
public Page<Order> getAllOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "100") int size
) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 100));
    return orderRepository.findAll(pageable);
}
```

**Benefits:**
- ✅ Memory: 500KB (vs 50MB) - **100x less**
- ✅ Network: 500KB (vs 50MB)
- ✅ Time: 35ms (vs 3,500ms) - **100x faster**
- ✅ Stable: Works with millions of orders

**Rule:** ALWAYS paginate list endpoints! Max 100 items per page.

---

## ❌ Anti-Pattern 10: Embedding Rapidly Changing Data

### Bad Example

```json
{
  "_id": "product123",
  "name": "iPhone 15",
  "price": 999.00,
  "inventory": 47,  // Changes every order!
  "reviews": [
    // 1,000 reviews embedded (grows constantly!)
  ]
}
```

### 🔴 Problems

1. **Write Amplification** - Every order rewrites entire product doc
2. **Growing Document** - Reviews array grows unbounded
3. **Performance** - Rewriting large docs is slow
4. **Concurrency** - Multiple orders = write conflicts

**Real Impact:**
- Document grows: 2KB → 2MB
- Update time: 5ms → 450ms
- **90x slower!**

---

### ✅ Good Example (Separate Rapidly Changing Data)

```json
// Product (relatively stable)
{
  "_id": "product123",
  "name": "iPhone 15",
  "price": 999.00,
  "sku": "PHONE-001"
}

// Inventory (separate, frequently updated)
{
  "_id": "inv_product123",
  "productId": "product123",
  "quantity": 47,
  "reserved": 3
}

// Reviews (separate, unbounded growth)
{
  "_id": "review1",
  "productId": "product123",
  "rating": 5,
  "text": "Great phone!"
}
```

**Benefits:**
- ✅ Product doc stays small
- ✅ Inventory updates are fast (small doc)
- ✅ Reviews can grow infinitely
- ✅ No write conflicts!

**Rule:** Separate stable data from rapidly changing data!

---

## 📊 Summary: Anti-Patterns Impact

| Anti-Pattern | Impact | Fix | Improvement |
|--------------|--------|-----|-------------|
| **Massive Arrays** | 16MB limit, 833x slower | Outlier Pattern | **833x faster** |
| **Unnecessary Refs** | N+1 queries | Embedding | **4x faster** |
| **No Indexes** | Table scans | Create indexes | **225x faster** |
| **Stale Computed** | Data inconsistency | Recalculate | 100% accurate |
| **_id as Business Key** | Conflicts | Separate field | Flexible |
| **Deep Nesting** | Query complexity | Flatten | Simple queries |
| **No Transactions** | Data loss | @Transactional | 100% consistent |
| **No Write Concern** | Data loss risk | w:majority | Durable |
| **No Pagination** | OOM crashes | Paginate | **100x less memory** |
| **Embed Changing Data** | Write amplification | Separate docs | **90x faster** |

---

## ✅ Quick Reference: Do This Instead!

1. **Arrays:** Use Outlier Pattern for 100+ items
2. **Relations:** Embed if ALWAYS accessed together
3. **Queries:** Index EVERY queried field
4. **Computed:** Recalculate on write
5. **Keys:** Auto-generate _id, index business keys
6. **Nesting:** Max 3 levels
7. **Multi-doc:** Use @Transactional
8. **Writes:** Use w:majority
9. **Lists:** Paginate (max 100/page)
10. **Changes:** Separate stable from changing data

---

**Learn from mistakes - avoid these anti-patterns!** ⚠️✅
