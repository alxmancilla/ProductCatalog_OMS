# MongoDB Schema Design Patterns in This Demo

This demo showcases **EIGHT MongoDB Schema Design Patterns** (including the Transaction Pattern) that solve real-world data modeling challenges.

---

## 🧮 Pattern 1: Computed Pattern

### What Is It?

Pre-calculate values and store them in the document so they're ready when clients request data.

### Where We Use It

**Order Total Calculation** - `OrderController.java`

### The Problem

Without the Computed Pattern:
```javascript
// Every time you query an order, you'd need to calculate:
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: {
      _id: "$_id",
      total: { $sum: { $multiply: ["$items.price", "$items.quantity"] } }
  }}
])
```
- Slow queries (calculation on every read)
- Complex aggregation pipelines
- More CPU usage

### Our Solution

```java
// Calculate ONCE when creating the order
BigDecimal total = order.getItems().stream()
    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
    .reduce(BigDecimal.ZERO, BigDecimal::add);

order.setTotal(total);  // Store it!
```

### The Result

```json
{
  "_id": "order123",
  "items": [
    { "name": "Laptop", "price": 1299.99, "quantity": 1 },
    { "name": "Mouse", "price": 29.99, "quantity": 2 }
  ],
  "total": 1359.97  ← Pre-calculated and stored!
}
```

### Benefits

✅ **Fast reads** - No calculation needed
✅ **Simple queries** - Just read the `total` field
✅ **Consistent** - Calculation logic in one place
✅ **Efficient** - Calculate once, read many times

### When to Use

- Values that are expensive to calculate
- Values that are read frequently
- Values that don't change after creation
- Aggregations (sum, average, count, etc.)

---

## 🎭 Pattern 2: Polymorphic Pattern

### What Is It?

Store documents with different fields in the same collection based on their type.

### Where We Use It

**Product Types** - `Product.java`

### The Problem

Traditional approach (separate tables/collections):
```
electronics_products
clothing_products
book_products
```
- Need to query multiple collections
- Complex union queries
- Hard to add new product types

### Our Solution

**One collection, flexible schema:**

```java
public class Product {
    private String type;  // "Electronics", "Clothing", "Book"

    // Common fields (all products)
    private String name;
    private BigDecimal price;

    // Electronics-specific
    private String warranty;
    private String brand;

    // Clothing-specific
    private String size;
    private String color;

    // Book-specific
    private String author;
    private String isbn;
}
```

### Example Documents

**Electronics:**
```json
{
  "_id": "prod1",
  "type": "Electronics",
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "warranty": "2 years",
  "brand": "TechCorp"
}
```

**Clothing:**
```json
{
  "_id": "prod2",
  "type": "Clothing",
  "name": "Cotton T-Shirt",
  "price": 29.99,
  "size": "L",
  "color": "Blue",
  "material": "100% Cotton"
}
```

**Book:**
```json
{
  "_id": "prod3",
  "type": "Book",
  "name": "MongoDB Guide",
  "price": 49.99,
  "author": "Jane Smith",
  "isbn": "978-1234567890",
  "pages": 350
}
```

### Benefits

✅ **Single collection** - Query all products at once
✅ **Flexible schema** - Each type has unique fields
✅ **Easy to extend** - Add new types without schema changes
✅ **Simpler queries** - No unions or joins needed

### When to Use

- Entities with common base fields but type-specific variations
- When you need to query across all types
- When types share similar behavior
- When new types are added frequently

---

## 📋 Pattern 3: Document Versioning

### What Is It?

Track schema changes over time to handle evolving requirements while maintaining backward compatibility.

### Where We Use It

**Order Schema Evolution** - `Order.java` (currently v3)
**Product Schema Evolution** - `Product.java` (currently v2) 🆕

### The Problem

Your schema needs to change, but you have existing data:
- Old orders use `customer: "John Doe"` (string)
- New orders need `customerId` + `customerName` (Subset Pattern)
- Products need new fields (`inventory`, `sku`, `description`) 🆕
- Can't migrate millions of documents instantly
- Application must handle both formats

### Our Solution

**Track the version:**

```java
// Order.java
public class Order {
    private Integer schemaVersion = 3;  // Track which version (v1 → v2 → v3)
    private String customerId;          // v2 field
    private String customerName;        // v2 field
    private Boolean isLargeOrder;       // v3 field (Outlier Pattern)
}

// Product.java 🆕
public class Product {
    private Integer schemaVersion = 2;  // Track which version (v1 → v2)
    private String description;         // v2 field (for AI matching)
    private Integer inventory;          // v2 field (for transactions)
    private String sku;                 // v2 field (unique identifier)
}
```

### Example Documents

**Order Version 1 (Old format):**
```json
{
  "_id": "order123",
  "customer": "John Doe",
  "orderDate": "2024-01-15",
  "items": [...],
  "total": 1299.99
}
```

**Order Version 3 (Current format):**
```json
{
  "_id": "order456",
  "schemaVersion": 3,
  "customerId": "cust789",
  "customerName": "Jane Smith",
  "orderDate": "2024-02-20",
  "items": [...],
  "isLargeOrder": false,
  "totalItemCount": 2,
  "bucketCount": 0,
  "total": 899.99
}
```

**Product Version 2 (Current format):** 🆕
```json
{
  "_id": "prod123",
  "schemaVersion": 2,
  "name": "Laptop Pro 15",
  "description": "High-performance laptop for professionals",
  "price": 1299.99,
  "category": "Electronics",
  "inventory": 50,
  "sku": "LAPTOP-PRO15-SG"
}
```

### Migration Strategy

**Gradual Migration:**
```java
// When reading orders
if (order.getSchemaVersion() == null || order.getSchemaVersion() == 1) {
    // Handle old format (v1)
    String customerName = order.getCustomer();  // Old field
    // Optionally migrate to v2 format
} else {
    // Handle new format (v2)
    String customerId = order.getCustomerId();
    String customerName = order.getCustomerName();
}
```

**All new orders** are created with `schemaVersion = 2`
**Old orders** can be migrated on-demand or in batches
**Application** handles both versions gracefully

### Benefits

✅ **Safe evolution** - Change schema without breaking existing data
✅ **Gradual migration** - No need to update all documents at once
✅ **Backward compatible** - Application handles old and new formats
✅ **Audit trail** - Know which version each document uses

### When to Use

- Schema needs to evolve over time
- Can't migrate all documents immediately
- Need to support multiple schema versions
- Want to track schema changes for debugging

### Current Schema Versions 🆕

| Collection | Current Version | Evolution |
|------------|----------------|-----------|
| **orders** | v3 | v1 (basic) → v2 (Subset Pattern) → v3 (Outlier Pattern) |
| **products** | v2 | v1 (basic) → v2 (inventory + SKU + description) |
| **customers** | No versioning | Schema stable since creation |
| **order_item_buckets** | No versioning | Created for Outlier Pattern |

**See [PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md) for detailed product schema evolution!**

---

## 📊 Pattern Comparison

| Pattern | Purpose | When to Use | Benefit |
|---------|---------|-------------|---------|
| **Computed** | Pre-calculate values | Expensive calculations, frequent reads | Fast queries |
| **Polymorphic** | Variable fields per type | Multiple entity types in one collection | Schema flexibility |
| **Document Versioning** | Track schema changes | Schema evolution, gradual migration | Safe updates |

---

## 🎯 How They Work Together in This Demo

### Order Document (All Three Patterns!)

```json
{
  "_id": "order789",
  "schemaVersion": 2,              ← DOCUMENT VERSIONING
  "customerId": "cust123",
  "customerName": "Alice Johnson",
  "orderDate": "2024-03-10",
  "items": [
    {
      "productId": "prod456",
      "name": "Laptop Pro 15",       ← From POLYMORPHIC product
      "price": 1299.99,
      "quantity": 1
    }
  ],
  "total": 1299.99                   ← COMPUTED PATTERN
}
```

### Product Document (Polymorphic Pattern)

```json
{
  "_id": "prod456",
  "type": "Electronics",             ← POLYMORPHIC PATTERN
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "warranty": "2 years",             ← Electronics-specific field
  "brand": "TechCorp"                ← Electronics-specific field
}
```

---

## 🚀 Try It Yourself

### Create a Polymorphic Product (Electronics)

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Electronics",
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50,
    "warranty": "2 years",
    "brand": "TechCorp"
  }'
```

### Create a Polymorphic Product (Clothing)

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Clothing",
    "name": "Cotton T-Shirt",
    "price": 29.99,
    "category": "Clothing",
    "inventory": 200,
    "size": "L",
    "color": "Blue",
    "material": "100% Cotton"
  }'
```

### Create an Order (See Computed Pattern in Action)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "<customer-id>",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "<product-id>",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 1
      }
    ]
  }'
```

**Notice:** The `total` field is automatically calculated and the `schemaVersion` is set to 2!

---

## 📚 Learn More

- **[MongoDB Schema Design Patterns](https://www.mongodb.com/blog/post/building-with-patterns-a-summary)** - Official MongoDB blog series
- **[Order.java](src/main/java/com/example/store/model/Order.java)** - See Computed + Versioning patterns
- **[Product.java](src/main/java/com/example/store/model/Product.java)** - See Polymorphic pattern
- **[OrderController.java](src/main/java/com/example/store/controller/OrderController.java)** - See Computed pattern implementation

---

## 🎯 Pattern 4: Outlier Pattern

### What Is It?

Handle cases where most documents are normal-sized, but a few are exceptionally large (outliers).

### Where We Use It

**Large Orders with 100+ Items** - `Order.java` + `OrderItemBucket.java`

### The Problem

- Most orders have 1-20 items (99% of cases)
- Some orders have 100+ items (1% of cases - outliers)
- MongoDB has a 16MB document size limit
- Large embedded arrays hurt performance

### Our Solution

**Adaptive Schema Design:**

```java
// Normal order (< 50 items): Embed items
if (itemCount < 50) {
    order.setItems(items);
    order.setIsLargeOrder(false);
}

// Large order (100+ items): Use bucketing
if (itemCount >= 100) {
    order.setIsLargeOrder(true);
    order.setTotalItemCount(itemCount);
    order.setBucketCount(bucketCount);
    order.setItems(null);  // Items stored in separate buckets

    // Create bucket documents
    for (int i = 0; i < bucketCount; i++) {
        OrderItemBucket bucket = new OrderItemBucket();
        bucket.setOrderId(order.getId());
        bucket.setBucketNumber(i);
        bucket.setItems(bucketItems);  // 50 items per bucket
        orderItemBucketRepository.save(bucket);
    }
}
```

### The Result

**Normal Order (20 items):**
```json
{
  "_id": "order123",
  "isLargeOrder": false,
  "items": [
    {"productId": "prod1", "name": "Laptop", "price": 1299.99, "quantity": 1},
    {"productId": "prod2", "name": "Mouse", "price": 29.99, "quantity": 2}
  ],
  "total": 1359.97
}
```

**Large Order (150 items):**

Main Order Document:
```json
{
  "_id": "order456",
  "isLargeOrder": true,
  "totalItemCount": 150,
  "bucketCount": 3,
  "items": null,  // ← Set to null for large orders
  "total": 15000.00
}
```

Bucket Documents (in `order_item_buckets` collection):
```json
{
  "_id": "order456_bucket_0",
  "orderId": "order456",
  "bucketNumber": 0,
  "items": [ /* 50 items */ ]
}
```

### Schema Validation 🆕

**Important:** MongoDB schema validation allows `items` to be `null` for large orders:

```java
// MongoSchemaValidation.java
.append("items", new Document()
    .append("bsonType", Arrays.asList("array", "null"))  // ← Allows null!
    .append("description", "Order items array (null for large orders using Outlier Pattern)")
)
```

**Why?** The `items` field is NOT in the required fields list to support the Outlier Pattern. This allows:
- ✅ Regular orders: `items` is an array
- ✅ Large orders: `items` is `null` (items stored in buckets)

### Benefits

✅ **Optimizes for common case** (99% of orders are small)
✅ **Handles outliers gracefully** (1% of orders are large)
✅ **Stays below 16MB limit** (buckets are manageable size)
✅ **Maintains performance** (small orders are fast)
✅ **Scalable** (can handle 1000s of items)
✅ **Schema validation supports both patterns** (array or null) 🆕

### When to Use

- Most documents are small, but a few are large
- You want to optimize for the common case
- You need to handle unbounded growth
- You want to stay well below MongoDB's limits

**See [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for comprehensive details!**

---

## 🔄 Pattern 8: Transaction Pattern 🆕

### What Is It?

Use MongoDB ACID transactions to ensure multiple operations across collections succeed or fail together atomically.

### Where We Use It

**Order Creation with Inventory Management** - `OrderTransactionService.java`

### The Problem

Without transactions:
```java
// ❌ DANGEROUS: Race conditions and inconsistent data!
Order order = orderRepository.save(order);  // Step 1: Create order

// Step 2: Decrement inventory (might fail!)
for (OrderItem item : order.getItems()) {
    Product product = productRepository.findById(item.getProductId());
    product.setInventory(product.getInventory() - item.getQuantity());
    productRepository.save(product);  // What if this fails?
}
// Result: Order created but inventory not updated! 😱
```

**Problems:**
- Order created even if inventory insufficient
- Partial updates if one product fails
- Race conditions with concurrent orders
- Data inconsistency

### Our Solution

```java
@Transactional  // ✅ All-or-nothing guarantee!
public Order createOrderWithInventoryUpdate(Order order) {
    // START TRANSACTION

    // 1. Validate all products exist
    for (OrderItem item : order.getItems()) {
        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(...));

        // 2. Check inventory availability
        if (product.getInventory() < item.getQuantity()) {
            throw new InsufficientInventoryException(...);
        }
    }

    // 3. Create order
    Order savedOrder = orderRepository.save(order);

    // 4. Decrement inventory for all products
    for (OrderItem item : order.getItems()) {
        productRepository.decrementInventory(
            item.getProductId(),
            item.getQuantity()
        );
    }

    // COMMIT (if all succeed) or ROLLBACK (if any fail)
    return savedOrder;
}
```

### The Result

**Successful Order:**
```json
// Order created
{
  "_id": "order123",
  "customerId": "cust456",
  "items": [
    { "productId": "prod789", "quantity": 2 }
  ],
  "total": 2599.98
}

// Inventory decremented atomically
{
  "_id": "prod789",
  "name": "Laptop Pro 15",
  "inventory": 8  // Was 10, now 8 (10 - 2)
}
```

**Insufficient Inventory (Transaction Rollback):**
```json
// HTTP 400 Bad Request
{
  "error": "Insufficient inventory for product 'Laptop Pro 15'. Available: 8, Requested: 100"
}

// No order created, inventory unchanged!
```

### Benefits

✅ **Atomicity** - All operations succeed or all fail together
✅ **Consistency** - Data always in valid state
✅ **Isolation** - Concurrent transactions don't interfere
✅ **Durability** - Committed changes are permanent
✅ **No Overselling** - Can't sell more than available
✅ **Automatic Rollback** - Failed operations don't leave partial data

### When to Use

- Operations that span multiple documents/collections
- Inventory management and stock control
- Financial transactions
- Any operation requiring all-or-nothing guarantee
- Preventing race conditions

### Requirements

⚠️ **MongoDB transactions require a replica set!**

```yaml
# docker-compose.yml
services:
  mongodb:
    image: mongo:8
    command: --replSet rs0  # Enable replica set
    healthcheck:
      test: ["CMD", "bash", "/docker-entrypoint-initdb.d/init-replica-set.sh"]
```

Even a single-node replica set works for development!

**See [TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md) for comprehensive details!**

---

## 🎓 Key Takeaways

1. **Embedding Pattern** = Store related data together (OrderItems in Orders)
2. **Subset Pattern** = Denormalize frequently accessed data
3. **Reference Pattern** = Link between collections when needed
4. **Computed Pattern** = Calculate once, read many times
5. **Polymorphic Pattern** = One collection, flexible schema
6. **Document Versioning** = Safe schema evolution
7. **Outlier Pattern** = Optimize for common case, handle outliers gracefully
8. **Transaction Pattern** 🆕 = ACID guarantees for multi-document operations

These patterns solve real-world problems and showcase MongoDB's flexibility and power! 🚀


