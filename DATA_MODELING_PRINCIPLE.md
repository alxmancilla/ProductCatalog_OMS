# MongoDB's Guiding Principle: "Data Accessed Together, Stored Together"

## The Fundamental Principle

> **"Data that is accessed together should be stored together."**

This principle is the foundation of MongoDB data modeling and drives better performance, simpler queries, and more maintainable applications.

---

## Relational vs. Document Approach

### ❌ Relational Database Approach (Normalized)

**Schema:**
```
orders table:
  - id
  - customer_id (FK)
  - order_date
  - total

order_items table:
  - id
  - order_id (FK)
  - product_id (FK)
  - quantity
  - price

customers table:
  - id
  - name
  - email
  - phone

products table:
  - id
  - name
  - price
  - category
```

**Query to get order details:**
```sql
SELECT 
  o.id, o.order_date, o.total,
  c.name as customer_name,
  oi.quantity, oi.price,
  p.name as product_name
FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.id = '123';
```

**Problems:**
- 3 JOIN operations
- Multiple table scans
- Slower performance
- Complex query
- Data scattered across 4 tables

---

### ✅ MongoDB Approach (Data Accessed Together, Stored Together)

**Schema:**
```javascript
// orders collection
{
  "_id": "order123",
  "customerId": "cust789",
  "customerName": "John Doe",        // ← Stored together (accessed together)
  "orderDate": "2024-01-15",
  "items": [                         // ← Stored together (accessed together)
    {
      "productId": "prod456",
      "name": "Laptop Pro 15",       // ← Stored together (accessed together)
      "price": 1299.99,              // ← Stored together (accessed together)
      "quantity": 1
    }
  ],
  "total": 1299.99
}

// customers collection (full data when needed)
{
  "_id": "cust789",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1-555-0123",
  "address": {...}
}

// products collection (current data when needed)
{
  "_id": "prod456",
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "category": "Electronics",
  "inventory": 50
}
```

**Query to get order details:**
```javascript
db.orders.findOne({ "_id": "order123" })
```

**Benefits:**
- ✅ ZERO joins
- ✅ Single query
- ✅ Instant retrieval
- ✅ Simple query
- ✅ All order data in one document

---

## How This Demo Applies the Principle

### 1. Embedding Pattern: OrderItems in Orders

**Access Pattern:** "When I need an order, I ALWAYS need its items"

**Decision:** Embed order items directly in the order document

**Code:**
```java
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private List<OrderItem> items;  // ← EMBEDDED (accessed together)
}
```

**Result:**
```javascript
// One query gets everything
db.orders.findOne({ "_id": "order123" })

// Returns complete order with all items - no joins!
```

---

### 2. Subset Pattern: Customer Name in Orders

**Access Pattern:** 
- "When displaying orders, I FREQUENTLY need customer name"
- "I RARELY need customer email/phone when viewing orders"
- "I SOMETIMES need full customer profile"

**Decision:** Store customer name with order (subset) + reference to full customer

**Code:**
```java
@Document(collection = "orders")
public class Order {
    private String customerId;      // ← Reference (when full data needed)
    private String customerName;    // ← Subset (accessed together with order)
}
```

**Result:**
```javascript
// 90% of queries - no lookup needed
db.orders.find({}, { customerName: 1, total: 1 })

// 10% of queries - lookup when needed
db.customers.findOne({ "_id": "cust789" })
```

---

### 3. Subset Pattern: Product Details in OrderItems

**Access Pattern:**
- "When viewing order history, I ALWAYS need product name/price at order time"
- "I RARELY need current product inventory when viewing old orders"
- "I SOMETIMES need current product details"

**Decision:** Store product name/price with order item (subset) + reference to current product

**Code:**
```java
public class OrderItem {
    private String productId;    // ← Reference (for current product data)
    private String name;         // ← Subset (historical snapshot)
    private BigDecimal price;    // ← Subset (price at order time)
}
```

**Result:**
```javascript
// Order history - instant (no lookup)
db.orders.find({ customerId: "cust789" })

// Current product data - when needed
db.products.findOne({ "_id": "prod456" })
```

---

## Decision Framework

### Ask These Questions:

1. **Is this data ALWAYS accessed together?**
   - YES → **Embed it** (OrderItems in Order)
   - NO → Continue to question 2

2. **Is this data FREQUENTLY accessed together?**
   - YES → **Use Subset Pattern** (customerName in Order)
   - NO → Continue to question 3

3. **Is this data RARELY accessed together?**
   - YES → **Use Reference only** (full customer data)

---

## Performance Impact

### Example: "Show last 100 orders"

**Relational (Normalized):**
```sql
-- Requires 3 joins for each of 100 orders
-- = 300 join operations
-- Query time: ~500ms
```

**MongoDB (Data Together):**
```javascript
db.orders.find().sort({ orderDate: -1 }).limit(100)
// Zero joins
// Query time: ~5ms
```

**Result: 100x faster!** 🚀

---

## Summary

MongoDB's principle **"Data accessed together, stored together"** leads to:

✅ **Faster queries** - No joins, single document retrieval  
✅ **Simpler code** - No complex join logic  
✅ **Better performance** - Reduced I/O operations  
✅ **Scalability** - Documents can be sharded easily  
✅ **Flexibility** - Can still access related data when needed  

**This demo showcases:**
- Embedding (items in orders)
- Subset Pattern (customer name, product details)
- References (full customer/product data)

All driven by the question: **"What data is accessed together?"**


