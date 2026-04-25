# 🎬 Comprehensive Demo Guide - Product Catalog + OMS

## Overview

This guide provides step-by-step instructions for delivering a **compelling demo** of the MongoDB-powered Product Catalog + Order Management System, showcasing all features using realistic data:

- **10 diverse customers** (STANDARD, GOLD, PLATINUM tiers)
- **200 products** (Electronics, Clothing, Books, Generic)
- **5,000 orders** spanning 2 years with realistic patterns
- **$6.8M total revenue** with seasonal trends

---

## 📋 Prerequisites

### 1. MongoDB Setup
```bash
# Ensure MongoDB is running as a replica set
mongosh --eval "rs.status()"
```

### 2. Start the Application
```bash
mvn spring-boot:run
```

Wait for the application to start (look for "Started DemoApplication").

### 3. Load Demo Data
```bash
# Generate the demo dataset
python3 demo-data/generate_demo_data.py

# Load data into MongoDB (takes ~5-10 minutes)
./demo-data/load-demo-data.sh

# Output:
# ✅ Customers:  10 loaded
# ✅ Products:   150 loaded
# ✅ Orders:     5000 loaded
```

---

## 🎯 Demo Flow (15 minutes)

### **Minute 0-2: Introduction & Architecture**

**Opening:**
> "Today I'll show you a production-ready Order Management System built with MongoDB, demonstrating why document databases are ideal for modern applications."

**Key Points:**
- Java 21 + Spring Boot 3.2 + MongoDB 8
- 8 MongoDB design patterns implemented
- ACID transactions for data consistency
- Advanced analytics with Aggregation Framework
- **2 years of realistic data** loaded!

**Show the dataset:**
```bash
# Quick stats
curl http://localhost:8080/customers | jq 'length'  # 10 customers
curl http://localhost:8080/products | jq 'length'   # 200 products
curl http://localhost:8080/orders | jq 'length'     # 5000 orders
```

---

### **Minute 2-5: Interactive Web Interface**

**Open the web interface:**
```bash
open http://localhost:8080
```

**Demonstrate:**

#### 1. **Browse Customers** (30 seconds)
- Click "Get All Customers"
- Show different tiers: PLATINUM, GOLD, STANDARD
- Point out: "Notice customers from different cities, companies, industries"

#### 2. **Browse Products** (1 minute)
- Click "Get All Products"
- Show variety: Laptops ($3,499 MacBook), Books ($16.99), Clothing, Smart Home
- **Highlight Polymorphic Pattern:** Different product types with type-specific details
- Show inventory levels

#### 3. **Browse Orders** (1 minute)
- Click "Get All Orders"
- Show different statuses: DELIVERED, SHIPPED, CONFIRMED, PENDING, CANCELLED
- Show order totals ranging from $100 to $10,000+
- **Highlight Embedding Pattern:** Items embedded in orders

---

### **Minute 5-8: Core OMS Features**

#### 1. **Get Order by ID** (1 minute)
```bash
# Pick a random delivered order
ORDER_ID=$(curl -s http://localhost:8080/orders | jq -r '.[0].id')
curl http://localhost:8080/orders/$ORDER_ID | jq
```

**Show:**
- Complete order with all items embedded
- Status history (audit trail)
- Customer information (Subset Pattern)
- Product details denormalized
- **"Zero joins! Everything in one query!"**

#### 2. **Search & Filter** (2 minutes)

**By Status:**
```bash
# Find all shipped orders
curl "http://localhost:8080/orders?status=SHIPPED" | jq 'length'
# Result: ~500 orders
```

**By Customer:**
```bash
# Get a customer ID
CUSTOMER_ID=$(curl -s http://localhost:8080/customers | jq -r '.[0].id')

# Find all their orders
curl "http://localhost:8080/orders/search/by-customer?customerId=$CUSTOMER_ID" | jq
```

**By Date Range:**
```bash
# Orders in last 30 days
curl "http://localhost:8080/orders/search/by-date?startDate=2024-03-01&endDate=2024-03-31" | jq 'length'
```

**By Price Range:**
```bash
# High-value orders (>$5000)
curl "http://localhost:8080/orders/search/by-total?minTotal=5000" | jq 'length'
```

---

### **Minute 8-11: Analytics & Business Intelligence** ⭐

**"Now let's see MongoDB's Aggregation Framework in action for business analytics!"**

#### 1. **Revenue by Status** (2 minutes)
```bash
curl http://localhost:8080/analytics/orders/revenue-by-status | jq
```

**Expected Output:**
```json
[
  {
    "status": "DELIVERED",
    "totalRevenue": 5000000.00,
    "orderCount": 3450,
    "averageOrderValue": 1449.28
  }
]
```

**Talking Points:**
- "This uses MongoDB's $group and $sum operators"
- "Equivalent to SQL: SELECT status, SUM(total), COUNT(*) GROUP BY status"
- "But executed server-side on MongoDB!"
- **"70% of orders delivered, $5M+ revenue!"**

#### 2. **Top Customers** (2 minutes)
```bash
curl "http://localhost:8080/analytics/orders/top-customers?limit=5" | jq
```

**Show:**
- VIP customers (PLATINUM tier members likely at top)
- Total spending per customer
- Order frequency
- **"Perfect for loyalty programs and customer segmentation!"**

#### 3. **Popular Products** (3 minutes) 🌟 **HIGHLIGHT THIS!**
```bash
curl "http://localhost:8080/analytics/orders/popular-products?limit=10" | jq
```

**MongoDB Pipeline Visualization:**
```javascript
// Stage 1: Unwind embedded items array
{ $unwind: "$items" }
// Takes: { items: [{productId: "A", qty: 2}, {productId: "B", qty: 1}] }
// Produces:
//   { items: {productId: "A", qty: 2} }
//   { items: {productId: "B", qty: 1} }

// Stage 2: Calculate revenue per item
{ $addFields: {
    itemRevenue: { $multiply: ["$items.price", "$items.quantity"] }
}}

// Stage 3: Group by product
{ $group: {
    _id: "$items.productId",
    totalQuantitySold: { $sum: "$items.quantity" },
    totalRevenue: { $sum: "$itemRevenue" }
}}

// Stage 4: Sort by popularity
{ $sort: { totalQuantitySold: -1 } }
```

**Talking Points:**
- 🌟 **"$unwind is UNIQUE to document databases!"**
- **"We 'explode' embedded arrays into separate documents for analysis"**
- **"Calculate revenue with $multiply - arithmetic in the database!"**
- **"NO JOINS! Items are embedded in orders"**
- "This would require complex JOINs in SQL"
- "MacBook Pro and high-end electronics likely dominate sales"

#### 4. **Daily Revenue Trends** (2 minutes)
```bash
curl "http://localhost:8080/analytics/orders/daily-revenue?days=30" | jq
```

**Show:**
- Last 30 days of revenue
- Date formatting with $dateToString
- Perfect for time-series charting
- **"Notice seasonal patterns - holidays spike revenue!"**

---

### **Minute 11-13: ACID Transactions**

**"MongoDB provides ACID guarantees just like traditional databases!"**

#### Demo: Order Creation with Inventory Validation
```bash
# Get a product ID
PRODUCT_ID=$(curl -s http://localhost:8080/products | jq -r '.[0].id')
CUSTOMER_ID=$(curl -s http://localhost:8080/customers | jq -r '.[0].id')

# Check current inventory
curl http://localhost:8080/products/$PRODUCT_ID | jq '.inventory'

# Create order (this decrements inventory)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "'$CUSTOMER_ID'",
    "items": [
      {
        "productId": "'$PRODUCT_ID'",
        "quantity": 2
      }
    ]
  }' | jq

# Check inventory again - should be decremented!
curl http://localhost:8080/products/$PRODUCT_ID | jq '.inventory'
```

**Talking Points:**
- "Order creation + inventory update = ONE transaction"
- "If ANY step fails, EVERYTHING rolls back"
- "Impossible to have orders without inventory changes"
- "Production-ready data consistency!"

---

### **Minute 13-14: MongoDB Design Patterns Showcase**

**Quickly highlight the 8 patterns:**

1. **Embedding Pattern** ✅
   - Items embedded in orders
   - Status history embedded
   - **Benefit:** 1 query instead of 3!

2. **Subset Pattern** ✅
   - Customer name denormalized to orders
   - Product details cached in order items
   - **Benefit:** Fast queries without joins

3. **Reference Pattern** ✅
   - Orders → Customer ID
   - Items → Product ID
   - **Benefit:** Avoid data duplication

4. **Computed Pattern** ✅
   - Order total pre-calculated
   - **Benefit:** No runtime calculation

5. **Polymorphic Pattern** ✅
   - Electronics, Clothing, Books, Generic
   - Different fields per type
   - **Benefit:** Flexible schema

6. **Document Versioning** ✅
   - schemaVersion field
   - **Benefit:** Handle schema evolution

7. **Outlier Pattern** ✅
   - Large orders (100+ items) use bucketing
   - **Benefit:** Avoid 16MB document limit

8. **Transaction Pattern** ✅
   - Order + Inventory updated atomically
   - **Benefit:** Data consistency

---

### **Minute 14-15: Wrap Up & Q&A**

**Summary:**
✅ **Real dataset:** 10 customers, 200 products, 5,000 orders, $6.8M revenue
✅ **MongoDB patterns:** 8 production-ready patterns implemented
✅ **ACID transactions:** Order + inventory atomically consistent
✅ **Analytics:** Powerful aggregation framework for BI
✅ **Performance:** Zero joins, embedded data, server-side aggregation

**Key Takeaways:**
1. **"Data accessed together, stored together"** - MongoDB's guiding principle
2. **Document model** = Natural mapping to objects
3. **Aggregation Framework** = Powerful analytics without ETL
4. **ACID transactions** = Enterprise-grade consistency
5. **Flexible schema** = Polymorphic data, easy evolution

**Next Steps:**
- Try the interactive web interface
- Explore the analytics endpoints
- Review the source code
- Check out the documentation

---

## 🔥 Advanced Demo Scenarios

### Scenario 1: Order Lifecycle (5 minutes)

**Demonstrate complete order flow:**

```bash
# 1. Create order
ORDER_JSON=$(cat <<EOF
{
  "customerId": "$CUSTOMER_ID",
  "items": [
    {"productId": "$PRODUCT_ID", "quantity": 1}
  ]
}
EOF
)

ORDER_ID=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d "$ORDER_JSON" | jq -r '.id')

# 2. Confirm order
curl -X PUT http://localhost:8080/orders/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "CONFIRMED",
    "changedBy": "admin@example.com",
    "reason": "Payment confirmed"
  }' | jq

# 3. Ship order
curl -X PUT http://localhost:8080/orders/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "SHIPPED",
    "changedBy": "warehouse@example.com",
    "reason": "Package shipped via FedEx"
  }' | jq

# 4. Deliver order
curl -X PUT http://localhost:8080/orders/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "DELIVERED",
    "changedBy": "carrier@example.com",
    "reason": "Package delivered to customer"
  }' | jq

# 5. View complete status history
curl http://localhost:8080/orders/$ORDER_ID | jq '.statusHistory'
```

**Talking Points:**
- Complete audit trail
- Every status change tracked with who, when, why
- Embedded status history (no separate audit table!)

### Scenario 2: Order Modification (3 minutes)

**Show inventory delta calculation:**

```bash
# Get current order
curl http://localhost:8080/orders/$ORDER_ID | jq '.items'

# Update order (change quantities)
curl -X PUT http://localhost:8080/orders/$ORDER_ID/items \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": "'$PRODUCT_ID'", "quantity": 5}
    ],
    "updatedBy": "customer@example.com",
    "reason": "Customer requested more items"
  }' | jq

# Check inventory adjustment
curl http://localhost:8080/products/$PRODUCT_ID | jq '.inventory'
```

**Talking Points:**
- Smart delta calculation: new quantity - old quantity
- Inventory adjusted accordingly
- All in one ACID transaction

### Scenario 3: Item Removal (2 minutes)

**Show quantity: 0 removal:**

```bash
# Remove item by setting quantity to 0
curl -X PUT http://localhost:8080/orders/$ORDER_ID/items \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": "'$PRODUCT_ID'", "quantity": 0}
    ],
    "updatedBy": "customer@example.com",
    "reason": "Customer removed item"
  }' | jq

# Inventory fully restored!
```

---

## 📊 Dataset Statistics

### Customer Distribution
- PLATINUM: 3 customers (30%)
- GOLD: 4 customers (40%)
- STANDARD: 3 customers (30%)

### Product Categories
- Electronics: 69 products (46%)
- Clothing: 35 products (23%)
- Books: 26 products (17%)
- Generic: 20 products (13%)

### Order Status Distribution
- DELIVERED: ~3,450 orders (69%)
- SHIPPED: ~514 orders (10%)
- CONFIRMED: ~405 orders (8%)
- PENDING: ~264 orders (5%)
- CANCELLED: ~367 orders (7%)

### Revenue Insights
- **Total Revenue:** $6,792,801.07
- **Average Order Value:** $1,466.18
- **Total Items Sold:** 17,521
- **Peak Season:** November-December (holiday boost)
- **Secondary Peak:** August-September (back-to-school)

---

## 🎯 Demo Tips

1. **Start with the web interface** - Most visual and engaging
2. **Use the analytics endpoints** - Show MongoDB's power
3. **Demonstrate transactions** - Prove ACID compliance
4. **Highlight $unwind** - Unique to document databases
5. **Show the audit trail** - Embedded status history
6. **Compare to SQL** - Use the MongoDB vs SQL examples
7. **Emphasize "zero joins"** - Key MongoDB advantage
8. **Show real numbers** - $6.8M revenue, 5000 orders
9. **Mention production-ready** - 2 years of data, realistic patterns
10. **End with Q&A** - Let audience explore

---

## 🚀 Quick Start Commands

```bash
# Load everything
python3 demo-data/generate_demo_data.py && ./demo-data/load-demo-data.sh

# Open web interface
open http://localhost:8080

# Quick analytics
curl http://localhost:8080/analytics/orders/revenue-by-status | jq
curl http://localhost:8080/analytics/orders/top-customers?limit=5 | jq
curl http://localhost:8080/analytics/orders/popular-products?limit=10 | jq
```

---

**Ready to deliver an amazing MongoDB demo!** 🎉

