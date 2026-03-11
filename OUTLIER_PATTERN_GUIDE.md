# MongoDB Outlier Pattern - Handling Large Arrays

## 🎯 What Is the Outlier Pattern?

The **Outlier Pattern** is a MongoDB schema design pattern that handles cases where **most documents are normal-sized, but a few are exceptionally large** (outliers).

### The Problem

You have a collection where:
- **99% of documents** are small (e.g., orders with 1-20 items)
- **0.9% of documents** are medium (e.g., orders with 50-100 items)
- **0.1% of documents** are large (e.g., orders with 100+ items)

If you optimize for the outliers, you hurt performance for the common case.  
If you ignore the outliers, you hit MongoDB's limits or performance issues.

---

## 📏 MongoDB's Constraints

### Hard Limits
- **Maximum document size:** 16 MB (BSON limit)
- **Theoretical max array size:** ~30,000-80,000 items (depending on item size)

### Practical Concerns (Before Hitting Limits)
- **Performance degradation** with large arrays (50+ items)
- **Memory pressure** from loading large documents
- **Network overhead** transferring large documents
- **Index inefficiency** with large arrays
- **Update conflicts** with concurrent modifications

---

## 🎯 The Solution: Adaptive Schema Design

### Decision Framework

```
Order Item Count:
│
├─ 1-50 items (99% of orders)
│  └─> ✅ EMBED items in Order document
│      - Fast queries (single document read)
│      - Atomic updates
│      - Simple code
│
├─ 50-100 items (0.9% of orders)
│  └─> ⚠️ EMBED + FLAG as large
│      - Still embed for simplicity
│      - Set isLargeOrder = true
│      - Monitor performance
│
├─ 100-500 items (0.09% of orders)
│  └─> 🔄 BUCKETING PATTERN
│      - Split into buckets of 50 items
│      - Order summary in main document
│      - Items in separate bucket documents
│
└─ 500+ items (0.01% of orders - true outliers!)
   └─> 📋 EXTENDED REFERENCE PATTERN
       - Each item as separate document
       - Order summary in main document
       - Query items with pagination
```

---

## 💡 Implementation in This Demo

### Normal Order (< 50 items)

**Document Structure:**
```json
{
  "_id": "order123",
  "schemaVersion": 3,
  "customerId": "cust456",
  "customerName": "John Doe",
  "orderDate": "2024-03-10",
  "isLargeOrder": false,
  "items": [
    {
      "productId": "prod1",
      "name": "Laptop Pro 15",
      "price": 1299.99,
      "quantity": 1
    },
    {
      "productId": "prod2",
      "name": "Wireless Mouse",
      "price": 29.99,
      "quantity": 2
    }
  ],
  "total": 1359.97
}
```

**Benefits:**
- ✅ Single query retrieves complete order
- ✅ Atomic updates (order + items together)
- ✅ Simple code
- ✅ Fast performance

---

### Medium Order (50-99 items)

**Document Structure:**
```json
{
  "_id": "order456",
  "schemaVersion": 3,
  "customerId": "cust789",
  "customerName": "Jane Smith",
  "orderDate": "2024-03-10",
  "isLargeOrder": true,
  "totalItemCount": 75,
  "items": [
    ... 75 items embedded ...
  ],
  "total": 7500.00
}
```

**Strategy:**
- ✅ Still embed items (simpler code)
- ✅ Flag with `isLargeOrder = true`
- ✅ Monitor performance
- ✅ Can migrate to bucketing if needed

---

### Large Order (100+ items) - BUCKETING PATTERN

**Main Order Document:**
```json
{
  "_id": "order789",
  "schemaVersion": 3,
  "customerId": "cust123",
  "customerName": "Alice Johnson",
  "orderDate": "2024-03-10",
  "isLargeOrder": true,
  "totalItemCount": 150,
  "bucketCount": 3,
  "items": null,
  "total": 15000.00
}
```

**Bucket Documents (in `order_item_buckets` collection):**

Bucket 0 (items 1-50):
```json
{
  "_id": "order789_bucket_0",
  "orderId": "order789",
  "bucketNumber": 0,
  "items": [
    ... 50 items ...
  ]
}
```

Bucket 1 (items 51-100):
```json
{
  "_id": "order789_bucket_1",
  "orderId": "order789",
  "bucketNumber": 1,
  "items": [
    ... 50 items ...
  ]
}
```

Bucket 2 (items 101-150):
```json
{
  "_id": "order789_bucket_2",
  "orderId": "order789",
  "bucketNumber": 2,
  "items": [
    ... 50 items ...
  ]
}
```

**Benefits:**
- ✅ Handles 100s or 1000s of items
- ✅ Stays well below 16MB limit
- ✅ Efficient pagination
- ✅ Optimizes for common case

---

## 🚀 How It Works in Code

### Creating a Normal Order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust123",
    "customerName": "John Doe",
    "items": [
      {"productId": "prod1", "name": "Laptop", "price": 1299.99, "quantity": 1},
      {"productId": "prod2", "name": "Mouse", "price": 29.99, "quantity": 2}
    ]
  }'
```

**Result:**
- Items embedded in Order document
- `isLargeOrder = false`
- Single document saved

---

### Creating a Large Order (100+ items)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust456",
    "customerName": "Alice Johnson",
    "items": [
      ... 150 items ...
    ]
  }'
```

**Result:**
- Main Order document saved (without items)
- `isLargeOrder = true`
- `totalItemCount = 150`
- `bucketCount = 3`
- 3 bucket documents created in `order_item_buckets` collection

---

### Retrieving Items from a Large Order

```bash
# Get order summary (fast - no items)
curl http://localhost:8080/orders/order789

# Get all items (retrieves and combines all buckets)
curl http://localhost:8080/orders/order789/items
```

**Behind the scenes:**
1. Controller checks `isLargeOrder` flag
2. If true, queries `order_item_buckets` collection
3. Retrieves all buckets sorted by `bucketNumber`
4. Combines items from all buckets
5. Returns complete item list

---

## 📊 Performance Comparison

### Normal Order (20 items)
- **Read:** 1 query, ~5ms
- **Write:** 1 document, ~10ms
- **Document size:** ~5 KB

### Large Order (150 items) - WITHOUT Bucketing
- **Read:** 1 query, ~50ms
- **Write:** 1 document, ~100ms
- **Document size:** ~50 KB
- **Risk:** Performance degradation, memory pressure

### Large Order (150 items) - WITH Bucketing
- **Read (summary):** 1 query, ~5ms
- **Read (all items):** 4 queries (1 order + 3 buckets), ~20ms
- **Write:** 4 documents (1 order + 3 buckets), ~40ms
- **Document size:** Order ~2 KB, each bucket ~17 KB
- **Benefits:** Better performance, scalability, stays below limits

---

## 🎯 Key Takeaways

### When to Use the Outlier Pattern

✅ **Use it when:**
- Most documents are small, but a few are large
- You want to optimize for the common case
- You need to handle unbounded growth
- You want to stay well below MongoDB's 16MB limit

❌ **Don't use it when:**
- All documents are consistently large (use bucketing for all)
- All documents are consistently small (just embed)
- You need atomic updates across buckets (use transactions)

### Design Principles

1. **Optimize for the common case** (99% of orders)
2. **Handle outliers gracefully** (1% of orders)
3. **Monitor and adapt** (flag medium orders for future migration)
4. **Keep it simple** (don't over-engineer for rare cases)

---

## 🔍 MongoDB Queries

### Find all large orders
```javascript
db.orders.find({ "isLargeOrder": true })
```

### Get buckets for a specific order
```javascript
db.order_item_buckets.find({ "orderId": "order789" }).sort({ "bucketNumber": 1 })
```

### Get a specific bucket (pagination)
```javascript
db.order_item_buckets.findOne({ "orderId": "order789", "bucketNumber": 0 })
```

### Count items across all buckets
```javascript
db.order_item_buckets.aggregate([
  { $match: { "orderId": "order789" } },
  { $unwind: "$items" },
  { $count: "totalItems" }
])
```

---

## 📚 Related Patterns

- **Embedding Pattern:** Store related data together (used for normal orders)
- **Referencing Pattern:** Link to data in another collection (used for buckets)
- **Computed Pattern:** Pre-calculate totals (used for order total)
- **Subset Pattern:** Store frequently accessed data (used for customer name)
- **Document Versioning:** Track schema changes (used for schema evolution)

---

## 🎓 Learn More

- [MongoDB Schema Design Patterns](https://www.mongodb.com/blog/post/building-with-patterns-a-summary)
- [MongoDB Document Size Limits](https://docs.mongodb.com/manual/reference/limits/)
- [Bucketing Pattern](https://www.mongodb.com/blog/post/building-with-patterns-the-bucket-pattern)

---

**This pattern is demonstrated in:**
- `src/main/java/com/example/store/model/Order.java` - Order model with outlier fields
- `src/main/java/com/example/store/model/OrderItemBucket.java` - Bucket model
- `src/main/java/com/example/store/controller/OrderController.java` - Bucketing logic

