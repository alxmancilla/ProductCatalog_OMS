# MongoDB Schema Design Patterns in This Demo

This demo showcases **FOUR official MongoDB Schema Design Patterns** that solve real-world data modeling challenges.

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

**Order Schema Evolution** - `Order.java`

### The Problem

Your schema needs to change, but you have existing data:
- Old orders use `customer: "John Doe"` (string)
- New orders need `customerId` + `customerName` (Subset Pattern)
- Can't migrate millions of documents instantly
- Application must handle both formats

### Our Solution

**Track the version:**

```java
public class Order {
    private Integer schemaVersion = 2;  // Track which version
    private String customerId;          // v2 field
    private String customerName;        // v2 field
}
```

### Example Documents

**Version 1 (Old format):**
```json
{
  "_id": "order123",
  "customer": "John Doe",
  "orderDate": "2024-01-15",
  "items": [...],
  "total": 1299.99
}
```

**Version 2 (New format):**
```json
{
  "_id": "order456",
  "schemaVersion": 2,
  "customerId": "cust789",
  "customerName": "Jane Smith",
  "orderDate": "2024-02-20",
  "items": [...],
  "total": 899.99
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
  "items": null,
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

### Benefits

✅ **Optimizes for common case** (99% of orders are small)
✅ **Handles outliers gracefully** (1% of orders are large)
✅ **Stays below 16MB limit** (buckets are manageable size)
✅ **Maintains performance** (small orders are fast)
✅ **Scalable** (can handle 1000s of items)

### When to Use

- Most documents are small, but a few are large
- You want to optimize for the common case
- You need to handle unbounded growth
- You want to stay well below MongoDB's limits

**See [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for comprehensive details!**

---

## 🎓 Key Takeaways

1. **Computed Pattern** = Calculate once, read many times
2. **Polymorphic Pattern** = One collection, flexible schema
3. **Document Versioning** = Safe schema evolution
4. **Outlier Pattern** = Optimize for common case, handle outliers gracefully

These patterns solve real-world problems and showcase MongoDB's flexibility! 🚀


