# Demo Data - Quick Start Guide

## 🎯 Overview

This directory contains scripts and data to generate and load a comprehensive demo dataset:
- **10 Customers** with diverse tiers and profiles
- **200 Products** across Electronics, Clothing, Books, and Generic categories
- **5,000 Orders** spanning 2 years with realistic patterns
- **$6.8M Revenue** with seasonal trends

---

## 🚀 Quick Start

### Step 1: Ensure Application is Running

```bash
# Start the application (in a separate terminal)
cd /Users/mancilla/sw/ProductCatalog_OMS
mvn spring-boot:run
```

Wait for the application to fully start (look for "Started DemoApplication").

### Step 2: Generate the Dataset

```bash
python3 demo-data/generate_demo_data.py
```

**Output:**
```
✅ Generated 150 products
✅ Generated 5000 orders
📊 Total Revenue: $6,792,801.07
```

**Files created:**
- `customers.json` - 10 customers (already exists)
- `products-all.json` - 200 products with SKUs
- `orders-template.json` - 5000 orders with placeholders

### Step 3: Load the Data

```bash
./demo-data/load-demo-data.sh
```

**This will:**
1. Load 10 customers → get actual MongoDB IDs
2. Load 200 products → get actual MongoDB IDs
3. Replace placeholders in orders with actual IDs
4. Load 5,000 orders in batches

**Expected time:** 5-10 minutes

---

## 🐛 Troubleshooting

### Issue: Products not loading (0 products loaded)

**Symptom:**
```
✅ Loaded 0 products
   - Electronics: 69
   - Clothing: 35
```

**Cause:** The product data structure doesn't match the API expectations.

**Solution:**
The dataset has been fixed to include:
- `sku` field (required)
- `electronicsDetails.warranty` as String (e.g., "12 months")
- Removed invalid fields (`warrantyMonths`, `powerConsumption`)

**To verify the fix:**
```bash
# Check first product has correct structure
jq '.[0]' demo-data/products-all.json

# Should show:
{
  "name": "MacBook Pro 16\" M3 Max",
  "sku": "LAP-0001",
  "electronicsDetails": {
    "brand": "Apple",
    "warranty": "12 months"
  }
}
```

### Issue: Server returns 500 errors

**Possible causes:**
1. Application not running
2. MongoDB not running as replica set
3. Duplicate SKUs from previous tests

**Solutions:**

**1. Restart the application:**
```bash
# Stop current instance (Ctrl+C in terminal running mvn)
# Start fresh
mvn clean spring-boot:run
```

**2. Clear existing products (if duplicate SKUs):**
```bash
# Connect to MongoDB
mongosh

# Switch to database
use product_catalog

# Drop products collection
db.products.drop()

# Exit
exit
```

**3. Verify MongoDB replica set:**
```bash
mongosh --eval "rs.status()"
```

### Issue: Orders failing to load

**Cause:** Product/Customer IDs not properly replaced

**Solution:**
The loading script automatically:
1. Captures customer IDs when creating customers
2. Captures product IDs when creating products  
3. Runs Python script to replace placeholders
4. Loads orders with actual IDs

If this fails, check:
```bash
# Verify customers loaded
curl http://localhost:8080/customers | jq 'length'  # Should be 10

# Verify products loaded
curl http://localhost:8080/products | jq 'length'   # Should be 200
```

---

## 📁 File Descriptions

| File | Description |
|------|-------------|
| `customers.json` | 10 pre-defined customers (PLATINUM, GOLD, STANDARD tiers) |
| `generate_demo_data.py` | Python script to generate products and orders |
| `products-all.json` | Generated: 200 products with correct schema |
| `orders-template.json` | Generated: 5000 orders with placeholder IDs |
| `orders-final.json` | Created during load: Orders with actual MongoDB IDs |
| `load-demo-data.sh` | Bash script to load everything into MongoDB |
| `README.md` | This file |

---

## ✅ Verification

After loading, verify the data:

```bash
# Check counts
curl http://localhost:8080/customers | jq 'length'  # 10
curl http://localhost:8080/products | jq 'length'   # 200  
curl http://localhost:8080/orders | jq 'length'     # 5000

# Try analytics
curl http://localhost:8080/analytics/orders/revenue-by-status | jq
curl http://localhost:8080/analytics/orders/top-customers?limit=5 | jq
curl http://localhost:8080/analytics/orders/popular-products?limit=10 | jq
```

---

## 📊 Dataset Statistics

### Products by Type
- Electronics: 69 products (46%) - $49 to $3,499
- Clothing: 35 products (23%) - $29 to $399
- Books: 26 products (17%) - $15.99 to $59.99
- Generic: 20 products (13%) - $19.99 to $99.99

### Orders by Status
- DELIVERED: ~3,450 (69%)
- SHIPPED: ~514 (10%)
- CONFIRMED: ~405 (8%)
- PENDING: ~264 (5%)
- CANCELLED: ~367 (7%)

### Revenue Insights
- **Total Revenue:** $6,792,801.07
- **Average Order:** $1,466.18
- **Total Items:** 17,521 units
- **Peak Months:** Nov-Dec (holidays), Aug-Sep (back-to-school)

---

## 🎬 Ready for Demo!

Once loaded, you can:
- Browse all entities via web interface: http://localhost:8080
- Search and filter 5,000 orders
- View analytics dashboards (V1: aggregation, V2: CQRS)
- Demonstrate ACID transactions
- Show MongoDB aggregation pipelines
- Highlight 8 design patterns
- Test CQRS materialized views (100x faster!)

**Next Steps:**
1. Rebuild CQRS views: `curl -X POST http://localhost:8080/admin/analytics/rebuild-all`
2. Test analytics V2: `curl "http://localhost:8080/api/v2/analytics/top-products?limit=10"`
3. See **WEBINAR_OUTLINE.md** for complete demo script
4. See **CQRS_IMPLEMENTATION_SUMMARY.md** for CQRS architecture details
