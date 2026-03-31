# OMS P0 Testing & Validation Guide

## 🧪 Complete Testing Suite for Order Management System

This guide covers all testing and validation for the **Priority 0 (P0) Order Management System features** implemented in this project.

---

## 📋 **Features Tested**

All P0 features have been implemented and are ready for testing:

| Feature | Status | Test Coverage |
|---------|--------|---------------|
| ✅ Order Creation | Complete | Automated + Manual |
| ✅ Order Status Management | Complete | Automated + Manual |
| ✅ Get Order by ID | Complete | Automated + Manual |
| ✅ Order Search & Filtering | Complete | Automated + Manual |
| ✅ Order Updates | Complete | Automated + Manual |
| ✅ Order Cancellation | Complete | Automated + Manual |

---

## 🚀 **Quick Start: Run All Tests**

### **1. Ensure Prerequisites**

```bash
# MongoDB must be running as a replica set for transactions
mongosh --eval "rs.status()"

# Application must be running
curl http://localhost:8080/actuator/health
```

### **2. Run Complete Test Suite**

```bash
# Run all P0 feature tests (7 test scenarios)
./test-oms-p0-features.sh

# Verify MongoDB indexes
./verify-mongodb-indexes.sh
```

---

## 📝 **Test Scripts Overview**

### **1. `test-oms-p0-features.sh`**

**Comprehensive end-to-end test** covering all P0 OMS features.

**What it tests:**
- ✅ Order creation with inventory decrement
- ✅ Get order by ID
- ✅ Order status updates (PENDING → CONFIRMED)
- ✅ Order search (by customer, status, combined)
- ✅ Order updates with inventory delta calculation
- ✅ Order cancellation with inventory restoration
- ✅ Error handling (invalid operations)

**How to run:**
```bash
./test-oms-p0-features.sh
```

**Expected output:**
- All tests pass with ✅ green checkmarks
- Inventory changes are tracked and verified
- Final inventory matches original values after cancellation

---

### **2. `verify-mongodb-indexes.sh`**

**Verifies MongoDB performance optimization** through indexes.

**What it checks:**
- ✅ All required indexes exist on `orders` collection
- ✅ All required indexes exist on `order_item_buckets` collection
- ✅ Queries use indexes (IXSCAN) instead of collection scans (COLLSCAN)

**How to run:**
```bash
./verify-mongodb-indexes.sh
```

**Expected output:**
- All indexes exist
- Query plans show `IXSCAN` for all test queries

---

### **3. `test-transactions.sh`**

**Tests basic transaction functionality** (already existed).

**What it tests:**
- ✅ Order creation with inventory decrement
- ✅ Insufficient inventory error handling
- ✅ Product not found error handling
- ✅ Multiple products in one order

**How to run:**
```bash
./test-transactions.sh
```

---

## 🎯 **Manual Testing Guide**

### **Test 1: Order Creation**

```bash
# Create a customer
CUSTOMER=$(curl -s -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Customer",
    "email": "test@example.com",
    "phone": "+1-555-TEST"
  }')
CUSTOMER_ID=$(echo "$CUSTOMER" | jq -r '.id')

# Create products
PRODUCT=$(curl -s -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "Test product for OMS",
    "price": 99.99,
    "category": "Test",
    "inventory": 50,
    "sku": "TEST-001"
  }')
PRODUCT_ID=$(echo "$PRODUCT" | jq -r '.id')

# Create order
ORDER=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"Test Customer\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"name\": \"Test Product\",
        \"price\": 99.99,
        \"quantity\": 5
      }
    ]
  }")
ORDER_ID=$(echo "$ORDER" | jq -r '.id')

echo "Order created: $ORDER_ID"

# Verify inventory was decremented
curl http://localhost:8080/products/$PRODUCT_ID | jq '.inventory'
# Should show: 45 (50 - 5)
```

---

### **Test 2: Order Status Management**

```bash
# Update order status to CONFIRMED
curl -X PUT "http://localhost:8080/orders/$ORDER_ID/status" \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "CONFIRMED",
    "changedBy": "admin@example.com",
    "reason": "Payment received",
    "metadata": {
      "paymentId": "pay_12345",
      "paymentMethod": "credit_card"
    }
  }' | jq

# View status history
curl "http://localhost:8080/orders/$ORDER_ID" | jq '.statusHistory'
```

---

### **Test 3: Get Order by ID**

```bash
# Retrieve the order
curl "http://localhost:8080/orders/$ORDER_ID" | jq

# Expected fields:
# - id
# - customerId
# - status
# - items[]
# - total
# - statusHistory[]
```

---

### **Test 4: Order Search & Filtering**

```bash
# Search by customer
curl "http://localhost:8080/orders?customerId=$CUSTOMER_ID" | jq

# Search by status
curl "http://localhost:8080/orders?status=CONFIRMED" | jq

# Search by customer and status
curl "http://localhost:8080/orders/search/by-customer?customerId=$CUSTOMER_ID&status=CONFIRMED" | jq

# Search by date range
START_DATE="2024-01-01T00:00:00"
END_DATE="2024-12-31T23:59:59"
curl "http://localhost:8080/orders/search/by-date?startDate=$START_DATE&endDate=$END_DATE" | jq

# Search by total range
curl "http://localhost:8080/orders/search/by-total?minTotal=100&maxTotal=1000" | jq
```

---

### **Test 5: Order Updates**

**Order updates support:**
- ✅ Modify item quantities
- ✅ Add new items
- ✅ Remove items (set `quantity: 0`)
- ✅ Automatic inventory delta calculation
- ✅ ACID transaction guarantees

```bash
# Change status back to PENDING (can only update PENDING orders)
curl -X PUT "http://localhost:8080/orders/$ORDER_ID/status" \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "PENDING",
    "changedBy": "admin@example.com",
    "reason": "Allow modifications"
  }'

# Test 5a: Update order items (increase quantity from 5 to 10)
curl -X PUT "http://localhost:8080/orders/$ORDER_ID/items" \
  -H "Content-Type: application/json" \
  -d "{
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"name\": \"Test Product\",
        \"price\": 99.99,
        \"quantity\": 10
      }
    ],
    \"updatedBy\": \"customer@example.com\",
    \"reason\": \"Customer increased quantity\"
  }" | jq

# Verify inventory delta (should decrement by 5 more)
curl "http://localhost:8080/products/$PRODUCT_ID" | jq '.inventory'
# Should show: 40 (45 - 5)

# Test 5b: Remove item by setting quantity to 0
curl -X PUT "http://localhost:8080/orders/$ORDER_ID/items" \
  -H "Content-Type: application/json" \
  -d "{
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"name\": \"Test Product\",
        \"price\": 99.99,
        \"quantity\": 0
      }
    ],
    \"updatedBy\": \"customer@example.com\",
    \"reason\": \"Customer removed all items\"
  }"

# This will fail with error: "Order must have at least one item with quantity > 0"
# To remove all items, use cancellation instead
```

---

### **Test 6: Order Cancellation**

```bash
# Cancel the order
curl -X POST "http://localhost:8080/orders/$ORDER_ID/cancel" \
  -H "Content-Type: application/json" \
  -d '{
    "cancelledBy": "customer@example.com",
    "reason": "Customer requested cancellation",
    "metadata": {
      "refundId": "ref_12345",
      "refundAmount": 999.90
    }
  }' | jq

# Verify order status is CANCELLED
curl "http://localhost:8080/orders/$ORDER_ID" | jq '.status'

# Verify inventory was fully restored
curl "http://localhost:8080/products/$PRODUCT_ID" | jq '.inventory'
# Should show: 50 (back to original)
```

---

## 🔍 **Validation Checklist**

### **Functional Tests**

- [ ] Order creation decrements inventory atomically
- [ ] Get order by ID returns complete order
- [ ] Status updates create audit trail entries
- [ ] Search by customer returns correct orders
- [ ] Search by status filters correctly
- [ ] Order updates calculate inventory deltas correctly
- [ ] Order cancellation restores inventory fully
- [ ] Invalid operations return proper error codes

### **Transaction Tests**

- [ ] Failed order creation doesn't change inventory
- [ ] Failed status update doesn't modify history
- [ ] Failed cancellation doesn't restore inventory
- [ ] Failed update doesn't apply partial changes

### **Performance Tests**

- [ ] All queries use indexes (verify with `verify-mongodb-indexes.sh`)
- [ ] No collection scans (COLLSCAN) detected
- [ ] Large order handling works (100+ items)

### **Business Rule Tests**

- [ ] Can only cancel PENDING/CONFIRMED orders
- [ ] Can only update PENDING orders
- [ ] Status transitions are validated
- [ ] Inventory never goes negative

---

## 📊 **MongoDB Best Practices Validation**

Run these checks to ensure best practices:

```bash
# 1. Check indexes exist
./verify-mongodb-indexes.sh

# 2. Verify transactions are enabled (replica set required)
mongosh --eval "rs.status()"

# 3. Check schema validation
mongosh product_catalog_oms --eval "db.getCollectionInfos({name: 'orders'})" | jq '.[0].options.validator'

# 4. View collection stats
mongosh product_catalog_oms --eval "db.orders.stats()" | jq
```

---

## ✅ **Success Criteria**

The P0 OMS implementation is considered successful when:

1. ✅ All automated tests pass (`test-oms-p0-features.sh`)
2. ✅ All indexes are created and used (`verify-mongodb-indexes.sh`)
3. ✅ Transactions rollback properly on errors
4. ✅ Inventory accuracy is maintained across all operations
5. ✅ Complete audit trails exist for all order changes
6. ✅ Business rules are enforced (status transitions, update restrictions)
7. ✅ Error messages are clear and actionable

---

## 🐛 **Troubleshooting**

### **Issue: Tests fail with transaction errors**

**Solution:** Ensure MongoDB is running as a replica set:
```bash
# Initialize replica set
mongosh --eval "rs.initiate()"

# Verify status
mongosh --eval "rs.status()"
```

### **Issue: Indexes not found**

**Solution:** Restart the application to trigger index creation:
```bash
# Indexes are created by OrderIndexConfiguration.java on startup
./mvnw spring-boot:run
```

### **Issue: Inventory mismatch**

**Solution:** This should never happen with transactions. If it does:
1. Check MongoDB logs for transaction errors
2. Verify replica set is healthy
3. Ensure @Transactional annotations are present on service methods

---

## 📚 **Related Documentation**

- [`OMS_P0_IMPLEMENTATION_PLAN.md`](OMS_P0_IMPLEMENTATION_PLAN.md) - Implementation details
- [`TRANSACTIONS_GUIDE.md`](TRANSACTIONS_GUIDE.md) - MongoDB transactions guide
- [`OUTLIER_PATTERN_GUIDE.md`](OUTLIER_PATTERN_GUIDE.md) - Large order handling
- [`README.md`](README.md) - Main project documentation

---

**All P0 features are fully implemented, tested, and ready for production use!** 🎉

