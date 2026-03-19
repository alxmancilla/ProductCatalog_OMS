# 🔄 MongoDB Transactions Guide

## Overview
This guide explains how MongoDB ACID transactions are implemented in the Product Catalog + Order Management System to ensure data consistency when creating orders and updating inventory.

---

## 🎯 What Are MongoDB Transactions?

MongoDB transactions provide **ACID guarantees** across multiple documents and collections:

- **Atomicity**: All operations succeed or all fail (no partial updates)
- **Consistency**: Data remains in a valid state
- **Isolation**: Concurrent transactions don't interfere with each other
- **Durability**: Committed changes are permanent

---

## 📋 Use Case: Order Creation with Inventory Management

### **The Problem**

Without transactions, creating an order could lead to data inconsistencies:

```
1. User places order for 5 laptops
2. Order is created ✅
3. Application crashes before updating inventory ❌
4. Result: Order exists but inventory not decremented!
```

### **The Solution: Transactions**

With transactions, either everything succeeds or everything rolls back:

```
START TRANSACTION
├─ 1. Validate all products exist
├─ 2. Check inventory availability
├─ 3. Create order document
├─ 4. Decrement inventory for all products
└─ COMMIT (if all succeed) or ROLLBACK (if any fail)
```

---

## 🏗️ Implementation Architecture

### **1. MongoConfig - Enable Transactions**

<augment_code_snippet path="src/main/java/com/example/store/config/MongoConfig.java" mode="EXCERPT">
````java
@Configuration
@EnableTransactionManagement
public class MongoConfig {
    
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
````
</augment_code_snippet>

### **2. OrderTransactionService - Transaction Logic**

<augment_code_snippet path="src/main/java/com/example/store/service/OrderTransactionService.java" mode="EXCERPT">
````java
@Service
public class OrderTransactionService {
    
    @Transactional
    public Order createOrderWithInventoryUpdate(Order order) {
        // 1. Validate products exist
        // 2. Check inventory
        // 3. Create order
        // 4. Update inventory
        // All in one transaction!
    }
}
````
</augment_code_snippet>

### **3. OrderController - Use Transaction Service**

<augment_code_snippet path="src/main/java/com/example/store/controller/OrderController.java" mode="EXCERPT">
````java
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private OrderTransactionService orderTransactionService;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order savedOrder = orderTransactionService.createOrderWithInventoryUpdate(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
}
````
</augment_code_snippet>

---

## ⚙️ MongoDB Replica Set Requirement

**IMPORTANT**: MongoDB transactions require a replica set configuration!

### **Why?**
- Transactions use the replica set's oplog for consistency
- Even a single-node replica set works for development

### **Configuration**

**docker-compose.yml:**
```yaml
services:
  mongodb:
    image: mongo:8
    command: ["--replSet", "rs0", "--bind_ip_all"]
    healthcheck:
      test: echo "try { rs.status() } catch (err) { rs.initiate({_id:'rs0',members:[{_id:0,host:'localhost:27017'}]}) }" | mongosh --quiet
```

**Verify replica set:**
```bash
mongosh
> rs.status()
```

---

## 🧪 Testing Transactions

### **Test 1: Successful Order (Inventory Decremented)**

```bash
# 1. Check initial inventory
curl http://localhost:8080/products | jq '.[] | select(.name=="Laptop Pro 15") | {name, inventory}'
# Output: { "name": "Laptop Pro 15", "inventory": 50 }

# 2. Create order for 2 laptops
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 2
      }
    ]
  }'

# 3. Check updated inventory
curl http://localhost:8080/products | jq '.[] | select(.name=="Laptop Pro 15") | {name, inventory}'
# Output: { "name": "Laptop Pro 15", "inventory": 48 }
```

✅ **Result**: Order created AND inventory decremented atomically!

---

### **Test 2: Insufficient Inventory (Transaction Rollback)**

```bash
# 1. Try to order more than available
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "customerName": "Jane Smith",
    "items": [
      {
        "productId": "PRODUCT_ID",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 100
      }
    ]
  }'

# Response:
{
  "success": false,
  "message": "Insufficient inventory for one or more products",
  "insufficientProducts": {
    "PRODUCT_ID": {
      "productName": "Laptop Pro 15",
      "requested": 100,
      "available": 48
    }
  }
}

# 2. Verify inventory unchanged
curl http://localhost:8080/products | jq '.[] | select(.name=="Laptop Pro 15") | {name, inventory}'
# Output: { "name": "Laptop Pro 15", "inventory": 48 }
```

✅ **Result**: Order NOT created AND inventory NOT changed!

---

## 🔍 Transaction Flow Diagram

```
User Request: Create Order
         │
         ▼
┌────────────────────────────────────┐
│ OrderController.createOrder()      │
└────────────┬───────────────────────┘
             │
             ▼
┌────────────────────────────────────┐
│ OrderTransactionService            │
│ @Transactional                     │
├────────────────────────────────────┤
│ START TRANSACTION                  │
│                                    │
│ 1. For each product:               │
│    ├─ Find product by ID           │
│    ├─ If not found → throw error   │
│    └─ Check inventory >= quantity  │
│                                    │
│ 2. If any insufficient:            │
│    └─ throw InsufficientInventory  │
│                                    │
│ 3. Save order to database          │
│                                    │
│ 4. For each product:               │
│    ├─ Decrement inventory          │
│    └─ Save updated product         │
│                                    │
│ COMMIT TRANSACTION                 │
└────────────┬───────────────────────┘
             │
             ▼
      Return Order ✅
```

---

## 🛡️ Exception Handling

### **ProductNotFoundException**
```json
{
  "success": false,
  "message": "Product not found with ID: 12345",
  "productId": "12345"
}
```

### **InsufficientInventoryException**
```json
{
  "success": false,
  "message": "Insufficient inventory for one or more products",
  "insufficientProducts": {
    "product1": {
      "productName": "Laptop Pro 15",
      "requested": 10,
      "available": 5
    },
    "product2": {
      "productName": "Wireless Mouse",
      "requested": 50,
      "available": 20
    }
  }
}
```

---

## 📊 Benefits

| Benefit | Description |
|---------|-------------|
| **Data Consistency** | Order and inventory always in sync |
| **No Overselling** | Can't sell more than available |
| **Automatic Rollback** | Failed operations don't leave partial data |
| **Race Condition Prevention** | Concurrent orders handled correctly |
| **Clear Error Messages** | Users know exactly what went wrong |

---

## 🚀 Next Steps

1. **Start MongoDB with replica set:**
   ```bash
   docker-compose up -d
   ```

2. **Verify replica set:**
   ```bash
   mongosh
   > rs.status()
   ```

3. **Start application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Test transactions:**
   ```bash
   ./test-transactions.sh
   ```

---

**MongoDB Transactions ensure your order system is reliable and consistent! 🎉**

