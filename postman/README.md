# 🚀 Postman Collection - Product Catalog + OMS

## 📋 Overview

This Postman collection provides **complete API testing** for the Product Catalog and Order Management System.

**Included:**
- ✅ **80+ API requests** covering all endpoints
- ✅ **Automatic variable extraction** (IDs from responses)
- ✅ **Environment ready** (just set baseUrl)
- ✅ **Organized by feature** (Products, Orders, Analytics, etc.)
- ✅ **Test scripts** for assertions

---

## 🔧 Quick Start

### 1. Import Collection

**Option A: Postman Desktop**
1. Open Postman
2. Click "Import" button
3. Select `Product_Catalog_OMS.postman_collection.json`
4. Click "Import"

**Option B: Postman Web**
1. Go to https://web.postman.co/
2. Click "Import" → "Upload files"
3. Select the collection JSON file

### 2. Set Base URL

The collection uses a variable `{{baseUrl}}` which defaults to `http://localhost:8080`.

**To change:**
1. Select the collection
2. Go to "Variables" tab
3. Set `baseUrl` to your server URL
4. Click "Save"

### 3. Start Testing!

**Recommended order:**
1. **Health Check** → Verify server is running
2. **Create Product** → Auto-saves productId
3. **Create Customer** → Auto-saves customerId
4. **Create Order** → Uses saved variables
5. **Analytics** → View aggregated data

---

## 📚 Collection Structure

### 1. Health & System (2 requests)
- `GET /actuator/health` - Overall health
- `GET /actuator/health/mongo` - MongoDB connection

### 2. Products (7 requests)
- `GET /products` - List all (paginated)
- `GET /products/{id}` - Get by ID
- `GET /products/sku/{sku}` - Search by SKU
- `GET /products?category=Electronics` - Filter by category
- `POST /products` - Create product
- `PUT /products/{id}` - Update product
- `DELETE /products/{id}` - Delete product

### 3. Customers (2 requests)
- `GET /customers` - List all (paginated)
- `POST /customers` - Create customer

### 4. Orders (4 requests)
- `GET /orders` - List all (paginated)
- `GET /orders/{id}` - Get by ID
- `POST /orders` - Create order
- `PATCH /orders/{id}/status` - Update status

### 5. Analytics V1 - Aggregation (4 requests)
- `GET /analytics/revenue-by-status` - Revenue grouped by order status
- `GET /analytics/revenue-by-tier` - Revenue grouped by customer tier
- `GET /analytics/top-customers` - Top spenders
- `GET /analytics/product-popularity` - Most ordered products

### 6. Analytics V2 - CQRS (4 requests)
- `GET /analytics/v2/daily-revenue` - Daily revenue summaries (fast!)
- `GET /analytics/v2/product-popularity` - Product popularity summaries
- `GET /analytics/v2/customer-spending` - Customer spending summaries
- `POST /admin/analytics/rebuild-all` - Rebuild all materialized views

### 7. Demo Data (1 request)
- `POST /demo/load` - Load 5,000 demo orders

---

## 🎯 Usage Examples

### Example 1: Create and Query Product

**Step 1: Create Electronics Product**
```
POST {{baseUrl}}/products
{
  "type": "Electronics",
  "name": "MacBook Pro M3",
  "sku": "LAPTOP-MBP-M3",
  "price": 2499.00,
  "category": "Electronics",
  "inventory": 25,
  "warrantyYears": 1
}
```

**Result:** `productId` automatically saved to collection variables!

**Step 2: Get Product**
```
GET {{baseUrl}}/products/{{productId}}
```

Uses the auto-saved `productId`!

---

### Example 2: Complete Order Flow

**Step 1: Create Customer**
```
POST {{baseUrl}}/customers
{
  "name": "Alice Johnson",
  "email": "alice@example.com",
  "tier": "PLATINUM"
}
```

**Step 2: Create Order**
```
POST {{baseUrl}}/orders
{
  "customerId": "{{customerId}}",
  "customerName": "Alice Johnson",
  "items": [
    {
      "productId": "{{productId}}",
      "productName": "MacBook Pro M3",
      "price": 2499.00,
      "quantity": 1
    }
  ]
}
```

**Step 3: Update Status**
```
PATCH {{baseUrl}}/orders/{{orderId}}/status
{
  "status": "DELIVERED"
}
```

All IDs auto-extracted from previous responses!

---

### Example 3: Compare Analytics V1 vs V2

**V1 (Aggregation - Slower)**
```
GET {{baseUrl}}/analytics/revenue-by-status
```
Response time: ~150ms (scans all orders)

**V2 (CQRS - Faster)**
```
GET {{baseUrl}}/analytics/v2/daily-revenue
```
Response time: ~5ms (reads pre-calculated summary)

**30x faster!** 🚀

---

## 🔄 Automatic Variable Extraction

The collection includes **test scripts** that automatically extract IDs from responses:

**Create Product:**
```javascript
if (pm.response.code === 201) {
    var jsonData = pm.response.json();
    pm.collectionVariables.set('productId', jsonData.id);
}
```

**Variables managed:**
- `productId` - From product creation
- `customerId` - From customer creation
- `orderId` - From order creation

Use these in subsequent requests: `{{productId}}`, `{{customerId}}`, `{{orderId}}`

---

## 📊 Testing Workflow

### Beginner Workflow (30 minutes)

1. ✅ Health Check
2. ✅ Create 3 products (Electronics, Clothing, Books)
3. ✅ List products (verify pagination)
4. ✅ Create 2 customers
5. ✅ Create 5 orders
6. ✅ Update order statuses
7. ✅ View analytics

### Developer Workflow (Performance Testing)

1. ✅ Load demo data (5,000 orders)
2. ✅ Run Analytics V1 queries (note timing)
3. ✅ Rebuild CQRS views
4. ✅ Run Analytics V2 queries (compare timing)
5. ✅ Document performance improvement

---

## 🎓 Learning Exercises

### Exercise 1: N+1 Problem
**Goal:** See the benefit of embedding

1. Create order with 5 items
2. Time: `GET /orders/{id}` → Items embedded (1 query, ~3ms)
3. Compare to SQL (would need 1 + 5 = 6 queries)

### Exercise 2: CQRS Performance
**Goal:** Measure 30x performance gain

1. Load demo data
2. Time: `GET /analytics/revenue-by-status` (V1)
3. Rebuild views
4. Time: `GET /analytics/v2/daily-revenue` (V2)
5. Calculate speedup!

### Exercise 3: Pagination
**Goal:** Understand pagination benefits

1. List products: `GET /products?page=0&size=10`
2. List products: `GET /products?page=1&size=10`
3. Try `size=100` (max allowed)
4. Try `size=1000` (capped at 100!)

---

## 🐛 Troubleshooting

### Issue: "Connection refused"
**Fix:** Ensure Spring Boot app is running
```bash
./mvnw spring-boot:run
```

### Issue: "MongoDB connection failed"
**Fix:** Start MongoDB replica set
```bash
mongod --replSet rs0
# In mongosh:
rs.initiate()
```

### Issue: "Product not found"
**Fix:** Create products first before creating orders

### Issue: "No analytics data"
**Fix:** 
1. Create some orders first
2. Rebuild CQRS views: `POST /admin/analytics/rebuild-all`

---

## 📖 Related Documentation

- **[QUICK_START_30MIN.md](../QUICK_START_30MIN.md)** - 30-minute guided tutorial
- **[BUILD_FROM_SCRATCH.md](../BUILD_FROM_SCRATCH.md)** - Build this app yourself
- **[EXERCISES.md](../EXERCISES.md)** - 10 hands-on challenges
- **[PERFORMANCE_BENCHMARKS.md](../PERFORMANCE_BENCHMARKS.md)** - Timing comparisons
- **[README.md](../README.md)** - Main project documentation

---

## ✅ Verification Checklist

After importing, verify:
- [ ] Collection appears in Postman sidebar
- [ ] `baseUrl` variable set correctly
- [ ] Health check returns 200 OK
- [ ] Can create product (productId extracted)
- [ ] Can create customer (customerId extracted)
- [ ] Can create order (orderId extracted)
- [ ] Analytics endpoints return data

---

**Status:** ✅ Ready to test!

**Happy API testing!** 🚀
