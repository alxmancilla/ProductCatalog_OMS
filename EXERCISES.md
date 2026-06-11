# 💪 MongoDB Hands-On Exercises
## 10 Practice Challenges from Beginner to Advanced

---

## 🎯 How to Use This Guide

Each exercise has:
- 🎓 **Difficulty Level** (Beginner/Intermediate/Advanced)
- ⏱️ **Estimated Time**
- 🎯 **Learning Objective**
- 💡 **Hints**
- ✅ **Solution** (expandable)

**Recommendation:** Do exercises in order - they build on each other!

---

## Exercise 1: Your First Document
**Level:** 🎓 Beginner | **Time:** ⏱️ 10 minutes

### Goal
Create a customer document using MongoDB Compass GUI.

### Steps
1. Open MongoDB Compass
2. Connect to `mongodb://localhost:27017`
3. Navigate to database: `product_catalog`
4. Click collection: `customers`
5. Click "Add Data" → "Insert Document"
6. Create a customer with your information

### Your Task
Create a customer with these fields:
- `name` - Your name
- `email` - Your email
- `tier` - "GOLD"
- `phone` - Your phone number

<details>
<summary>💡 Hint</summary>

```json
{
  "name": "Your Name",
  "email": "your@email.com",
  "tier": "GOLD",
  "phone": "+1-555-1234"
}
```

Click "Insert" - MongoDB auto-generates the `_id`!
</details>

### Learning Objective
- ✅ Understand documents are just JSON
- ✅ MongoDB auto-generates `_id`
- ✅ No schema required (flexible!)

### Verification
```bash
curl http://localhost:8080/customers
# Should see your customer in the list!
```

---

## Exercise 2: Embedding vs N+1 Queries
**Level:** 🎓 Beginner | **Time:** ⏱️ 15 minutes

### Goal
Compare MongoDB embedding vs SQL-style N+1 queries.

### Scenario
You need to display an order with its items.

### Part A: MongoDB Way (Embedding)
```bash
# Get order with embedded items (1 query)
time curl http://localhost:8080/orders/{id}
```

**Count the queries:** 1
**Time:** ~3ms

### Part B: Simulate SQL Way (N+1)
If items were in a separate collection:
```bash
# Query 1: Get order
time curl http://localhost:8080/orders/{id}

# Query 2-N: Get each item (if 3 items = 3 queries)
time curl http://localhost:8080/order-items?orderId={id}
```

**Count the queries:** 1 + N (where N = number of items)
**Time:** ~3ms + (3 × 3ms) = ~12ms

### Your Task
1. Create an order with 5 items
2. Time the MongoDB query (embedding)
3. Calculate hypothetical SQL time (1 + 5 queries)

<details>
<summary>✅ Solution</summary>

**MongoDB Embedding:**
- 1 query
- ~3-5ms
- Simple!

**SQL Simulation (N+1):**
- 6 queries (1 order + 5 items)
- ~18-30ms
- 5-6x slower!

**Learning:** Embedding eliminates N+1 problem!
</details>

### Learning Objective
- ✅ Understand N+1 query problem
- ✅ See embedding's performance benefit
- ✅ Know when to embed vs reference

---

## Exercise 3: Index Impact
**Level:** 🎓 Intermediate | **Time:** ⏱️ 20 minutes

### Goal
Measure the impact of indexes on query performance.

### Setup
```bash
# Open MongoDB shell
mongosh

# Switch to database
use product_catalog

# Create 1000 test documents
for (let i = 1; i <= 1000; i++) {
  db.test_products.insertOne({
    name: `Product ${i}`,
    sku: `SKU-${i}`,
    category: i % 10 == 0 ? "Electronics" : "Clothing",
    price: Math.random() * 1000
  });
}
```

### Part A: Query WITHOUT Index
```javascript
// Drop any existing indexes
db.test_products.dropIndexes();

// Time the query
db.test_products.find({ sku: "SKU-500" }).explain("executionStats")
```

**Look for:**
- `executionTimeMillis` - How long?
- `totalDocsExamined` - How many docs scanned?
- `executionStages.stage` - Should be "COLLSCAN" (table scan)

### Part B: Create Index
```javascript
db.test_products.createIndex({ sku: 1 });
```

### Part C: Query WITH Index
```javascript
db.test_products.find({ sku: "SKU-500" }).explain("executionStats")
```

**Look for:**
- `executionTimeMillis` - How long now?
- `totalDocsExamined` - How many now?
- `executionStages.stage` - Should be "IXSCAN" (index scan)

### Your Task
Calculate the performance improvement!

<details>
<summary>✅ Solution</summary>

**Without Index:**
- Time: ~450ms
- Docs examined: 1,000 (all!)
- Stage: COLLSCAN

**With Index:**
- Time: ~2ms
- Docs examined: 1
- Stage: IXSCAN

**Improvement:** 225x faster! 🚀

**Learning:** Indexes are CRITICAL for performance!
</details>

### Cleanup
```javascript
db.test_products.drop();
```

---

## Exercise 4: Design Your Schema
**Level:** 🎓 Intermediate | **Time:** ⏱️ 25 minutes

### Scenario
You're building a blog platform with:
- **Posts** (title, content, author, publish date)
- **Comments** (author, text, timestamp)

### Your Task
Design the schema! Should comments be:
- **A)** Embedded in post documents?
- **B)** Separate collection with references?

### Questions to Consider
1. How often are posts and comments accessed together?
2. How many comments per post (average)?
3. Can comments exist without a post?
4. Do you need to query comments independently?

<details>
<summary>💡 Hint</summary>

Use the decision tree:
- ALWAYS together? → Embed
- FREQUENTLY together? → Subset Pattern
- RARELY together? → Reference

Also consider:
- Expected comment count?
- 16MB document limit
- Query patterns
</details>

<details>
<summary>✅ Solution</summary>

**Recommended: Hybrid Approach (Subset Pattern)**

```json
{
  "_id": "post123",
  "title": "MongoDB Design Patterns",
  "content": "...",
  "author": "John Doe",
  "publishDate": "2024-01-15",
  "commentCount": 150,
  "recentComments": [
    {
      "author": "Alice",
      "text": "Great post!",
      "timestamp": "2024-01-16T10:30:00"
    }
    // Last 10 comments embedded
  ],
  "commentIds": ["comment1", "comment2", ...]  // References to all
}
```

**Separate Comments Collection:**
```json
{
  "_id": "comment1",
  "postId": "post123",
  "author": "Bob",
  "text": "Very helpful!",
  "timestamp": "2024-01-15T14:20:00"
}
```

**Why This Works:**
- ✅ Post view shows last 10 comments (fast!)
- ✅ Full comment thread requires 1 extra query (acceptable)
- ✅ Scales to 1000s of comments (no 16MB limit issue)
- ✅ Can query comments independently (analytics)

**Alternative for Low-Traffic Blogs:**
If average < 50 comments/post → Fully embed!

**Learning:** Real-world often uses hybrid approaches!
</details>

---

## Exercise 5: Aggregation Pipeline
**Level:** 🎓 Intermediate | **Time:** ⏱️ 30 minutes

### Goal
Build an aggregation pipeline to answer business questions.

### Scenario
You have orders with embedded items. Answer: "What's our revenue by product category?"

### Your Task
Build the aggregation pipeline!

**Steps:**
1. Unwind items array
2. Lookup product details
3. Group by category
4. Calculate total revenue
5. Sort by revenue descending

<details>
<summary>💡 Hint</summary>

```javascript
db.orders.aggregate([
  // Stage 1: Unwind items
  { $unwind: "$items" },
  
  // Stage 2: Lookup product details
  { $lookup: {
      from: "products",
      localField: "items.productId",
      foreignField: "_id",
      as: "product"
  }},
  
  // Stage 3: Unwind product (array to object)
  { $unwind: "$product" },
  
  // Stage 4: Group by category
  { $group: {
      _id: "$product.category",
      totalRevenue: { $sum: { $multiply: ["$items.price", "$items.quantity"] }},
      orderCount: { $sum: 1 }
  }},
  
  // Stage 5: Sort
  { $sort: { totalRevenue: -1 }}
])
```
</details>

<details>
<summary>✅ Solution & Explanation</summary>

**Complete Pipeline:**
```javascript
db.orders.aggregate([
  { $unwind: "$items" },
  { $lookup: {
      from: "products",
      localField: "items.productId",
      foreignField: "_id",
      as: "product"
  }},
  { $unwind: "$product" },
  { $group: {
      _id: "$product.category",
      totalRevenue: { $sum: { $multiply: ["$items.price", "$items.quantity"] }},
      orderCount: { $sum: 1 }
  }},
  { $sort: { totalRevenue: -1 }}
])
```

**Result:**
```json
[
  { "_id": "Electronics", "totalRevenue": 125340.50, "orderCount": 234 },
  { "_id": "Clothing", "totalRevenue": 45120.00, "orderCount": 156 },
  { "_id": "Books", "totalRevenue": 12450.00, "orderCount": 89 }
]
```

**Stages Explained:**
1. `$unwind` - Flattens items array (1 order with 3 items → 3 docs)
2. `$lookup` - Joins products collection (like SQL JOIN)
3. `$unwind` - Converts product array to object
4. `$group` - Groups by category, sums revenue
5. `$sort` - Orders by revenue descending

**Learning:** Aggregation Framework = SQL on steroids!
</details>

---

## Exercise 6: Transaction Rollback
**Level:** 🎓 Advanced | **Time:** ⏱️ 25 minutes

### Goal
Test that transactions rollback on failure.

### Scenario
Create an order but simulate an inventory check failure.

### Your Task
1. Create a product with inventory: 5
2. Try to order quantity: 10
3. Verify transaction rolled back (order NOT created, inventory unchanged)

### Test Script
```bash
# Step 1: Create product with limited inventory
PRODUCT_ID=$(curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Limited Item","sku":"LTD-001","price":99.99,"inventory":5}' \
  | jq -r '.id')

echo "Product ID: $PRODUCT_ID"
echo "Initial inventory: 5"

# Step 2: Try to order MORE than available
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"Test Customer\",
    \"items\": [{
      \"productId\": \"$PRODUCT_ID\",
      \"productName\": \"Limited Item\",
      \"price\": 99.99,
      \"quantity\": 10
    }]
  }"

# Expected: 400 Bad Request

# Step 3: Verify inventory unchanged
curl http://localhost:8080/products/$PRODUCT_ID | jq '.inventory'
# Expected: 5 (NOT decremented!)
```

<details>
<summary>✅ Solution & Verification</summary>

**Expected Results:**
1. Product created ✅
2. Order request returns 400 Bad Request ✅
3. Inventory still 5 ✅
4. No order created in database ✅

**Verify in MongoDB:**
```javascript
db.orders.find({ "items.productId": "<PRODUCT_ID>" }).count()
// Should be 0 (no orders created)

db.products.findOne({ _id: "<PRODUCT_ID>" }).inventory
// Should be 5 (unchanged)
```

**What Happened:**
1. Transaction started
2. Order creation attempted
3. Inventory check failed (10 > 5)
4. Exception thrown
5. **Transaction rolled back** - No changes persisted!

**Learning:** Transactions ensure data consistency!
</details>

---

## Exercise 7: Subset Pattern Implementation
**Level:** 🎓 Advanced | **Time:** ⏱️ 30 minutes

### Goal
Implement the Subset Pattern for customer data in orders.

### Current Problem
Every order query requires 2 queries:
```javascript
// Get order
const order = db.orders.findOne({ _id: "order123" });

// Get customer details
const customer = db.customers.findOne({ _id: order.customerId });
```

### Your Task
Refactor to use Subset Pattern:
1. Identify frequently accessed customer fields
2. Embed those fields in orders
3. Keep reference to full customer
4. Measure performance improvement

<details>
<summary>💡 Hint</summary>

**Frequently accessed:** name, email, tier
**Rarely accessed:** phone, address, payment methods

**New Order Schema:**
```json
{
  "_id": "order123",
  "customerId": "cust456",  // Reference (rare lookup)
  "customerName": "John Doe",  // Subset (frequent)
  "customerEmail": "john@example.com",  // Subset
  "customerTier": "PLATINUM",  // Subset
  "items": [...]
}
```
</details>

<details>
<summary>✅ Solution</summary>

**Implementation Steps:**

**1. Update Order Creation:**
```java
@PostMapping
public ResponseEntity<Order> create(@RequestBody OrderRequest request) {
    // Fetch full customer
    Customer customer = customerRepository.findById(request.getCustomerId())
        .orElseThrow(() -> new RuntimeException("Customer not found"));
    
    // Create order with subset
    Order order = new Order();
    order.setCustomerId(customer.getId());
    order.setCustomerName(customer.getName());  // Subset!
    order.setCustomerEmail(customer.getEmail());  // Subset!
    order.setCustomerTier(customer.getTier());  // Subset!
    order.setItems(request.getItems());
    
    return ResponseEntity.ok(orderRepository.save(order));
}
```

**2. Performance Comparison:**

**Before (Reference Only):**
```bash
# 2 queries every time
GET /orders/{id}  → 3ms
GET /customers/{customerId}  → 2ms
Total: 5ms
```

**After (Subset Pattern):**
```bash
# 1 query 90% of the time
GET /orders/{id}  → 3ms (has name, email, tier)
Total: 3ms

# 2 queries only when full customer needed (10% of time)
GET /orders/{id}  → 3ms
GET /customers/{customerId}  → 2ms (for full address, etc.)
Total: 5ms
```

**Net Improvement:**
- 90% of queries: 3ms (vs 5ms) = **40% faster**
- 10% of queries: 5ms (same)
- **Average: 3.2ms vs 5ms = 36% faster**

**Learning:** Subset Pattern optimizes for common case!
</details>

---

## Exercise 8: Build a CQRS Read Model
**Level:** 🎓 Advanced | **Time:** ⏱️ 45 minutes

### Goal
Create a materialized view for fast analytics queries.

### Scenario
Aggregation query for daily revenue takes 150ms. Build a CQRS read model to make it 5ms!

### Your Task
1. Design `DailyRevenueSummary` model
2. Create updater service
3. Test performance improvement

<details>
<summary>💡 Hint</summary>

**Read Model:**
```java
@Document(collection = "daily_revenue_summary")
public class DailyRevenueSummary {
    @Id
    private String id;  // "2024-01-15_DELIVERED"
    
    private LocalDate date;
    private String status;
    private BigDecimal totalRevenue;
    private Long orderCount;
}
```

**Updater:**
```java
@Service
public class ViewUpdater {
    public void onOrderCreated(Order order) {
        String id = order.getOrderDate().toLocalDate() + "_" + order.getStatus();
        
        DailyRevenueSummary summary = repository.findById(id)
            .orElse(new DailyRevenueSummary(id, date, status));
        
        summary.setTotalRevenue(
            summary.getTotalRevenue().add(order.getTotal())
        );
        summary.setOrderCount(summary.getOrderCount() + 1);
        
        repository.save(summary);
    }
}
```
</details>

<details>
<summary>✅ Complete Solution</summary>

See [CQRS_IMPLEMENTATION_SUMMARY.md](CQRS_IMPLEMENTATION_SUMMARY.md) for full implementation!

**Performance Results:**
- Aggregation: 150ms (scan 5000 orders)
- CQRS: 5ms (read 1 summary document)
- **Improvement: 30x faster!** 🚀

**Learning:** CQRS trades write complexity for read speed!
</details>

---

## Exercise 9: Handle the Outlier Pattern
**Level:** 🎓 Advanced | **Time:** ⏱️ 40 minutes

### Goal
Handle orders with 100+ items without hitting 16MB limit.

### Problem
Embedding 200 items in an order document might exceed 16MB limit.

### Your Task
Implement bucketing strategy:
- Orders < 100 items: Embed in order
- Orders ≥ 100 items: Split into buckets

<details>
<summary>✅ Solution</summary>

See [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for complete guide!

**Key Concepts:**
- 99% of orders: Normal embedding (fast!)
- 1% outliers: Bucket strategy (scalable!)
- Best of both worlds!

**Learning:** Optimize for common case, handle outliers gracefully!
</details>

---

## Exercise 10: Schema Migration
**Level:** 🎓 Advanced | **Time:** ⏱️ 35 minutes

### Goal
Migrate schema from v1 to v2 without downtime.

### Scenario
Add `phone` field to customers, but old documents don't have it.

### Your Task
1. Add `schemaVersion` field
2. Support both versions in code
3. Migrate lazily on read/write

<details>
<summary>✅ Solution</summary>

**1. Document Versioning:**
```java
@Document
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;  // v2 only
    private Integer schemaVersion = 2;
}
```

**2. Handle Both Versions:**
```java
public Customer findById(String id) {
    Customer customer = repository.findById(id).orElseThrow();
    
    if (customer.getSchemaVersion() == null || customer.getSchemaVersion() == 1) {
        // Migrate to v2
        customer.setSchemaVersion(2);
        customer.setPhone(null);  // or fetch from external system
        repository.save(customer);
    }
    
    return customer;
}
```

**3. Gradual Migration:**
- New documents: v2 from start
- Old documents: Migrate on read
- No downtime!
- Background job for bulk migration (optional)

See [PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md) for details!

**Learning:** Flexible schema enables zero-downtime migrations!
</details>

---

## 🎉 Congratulations!

You've completed all 10 exercises! You now understand:

✅ **Basic Concepts:**
- Documents are JSON
- Embedding pattern
- Index performance

✅ **Intermediate Skills:**
- Schema design decisions
- Aggregation pipelines
- Subset Pattern

✅ **Advanced Patterns:**
- ACID transactions
- CQRS read models
- Outlier Pattern
- Schema versioning

---

## 🚀 Next Steps

**Want More Practice?**
- Build your own app using these patterns
- Explore this demo's code
- Try the performance benchmarks

**Want to Learn More?**
- [SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md) - All 8 patterns
- [CQRS_IMPLEMENTATION_SUMMARY.md](CQRS_IMPLEMENTATION_SUMMARY.md) - 100x performance
- [ANTIPATTERNS.md](ANTIPATTERNS.md) - What NOT to do

**Ready for Production?**
- [SECURITY.md](SECURITY.md) - Security best practices
- [IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md) - Production checklist

---

**Time invested:** 4-5 hours
**Skills gained:** Production MongoDB patterns
**Achievement:** MongoDB Developer! 🏆
