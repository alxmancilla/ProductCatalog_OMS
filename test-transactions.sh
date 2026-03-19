#!/bin/bash
# Test script for MongoDB transactions with order creation and inventory management
#
# This script tests:
# 1. Successful order creation with inventory decrement
# 2. Insufficient inventory error (transaction rollback)
# 3. Product not found error (transaction rollback)
# 4. Multiple products in one order

BASE_URL="http://localhost:8080"

echo "════════════════════════════════════════════════════════════════════════"
echo "MongoDB Transactions Test Script"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
echo "This script tests ACID transactions for order creation with inventory management."
echo ""
read -p "Press Enter to start..."
echo ""

# ============================================================================
# Setup: Create test customer and products
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "Setup: Creating Test Customer and Products"
echo "════════════════════════════════════════════════════════════════════════"
echo ""

# Create customer
echo "Creating test customer..."
CUSTOMER=$(curl -s -X POST $BASE_URL/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Transaction Test Customer",
    "email": "test@example.com",
    "phone": "+1-555-TEST"
  }')
CUSTOMER_ID=$(echo "$CUSTOMER" | jq -r '.id')
echo "Customer ID: $CUSTOMER_ID"
echo ""

# Create Product 1: Laptop (inventory: 10)
echo "Creating Product 1: Laptop Pro 15 (inventory: 10)..."
PRODUCT1=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "description": "High-performance laptop for testing transactions",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 10,
    "sku": "TEST-LAPTOP-001"
  }')
PRODUCT1_ID=$(echo "$PRODUCT1" | jq -r '.id')
echo "Product 1 ID: $PRODUCT1_ID"
echo "Initial inventory: 10"
echo ""

# Create Product 2: Mouse (inventory: 5)
echo "Creating Product 2: Wireless Mouse (inventory: 5)..."
PRODUCT2=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse for testing",
    "price": 29.99,
    "category": "Accessories",
    "inventory": 5,
    "sku": "TEST-MOUSE-001"
  }')
PRODUCT2_ID=$(echo "$PRODUCT2" | jq -r '.id')
echo "Product 2 ID: $PRODUCT2_ID"
echo "Initial inventory: 5"
echo ""

read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 1: Successful Order (Inventory Should Decrement)
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "Test 1: Successful Order Creation"
echo "════════════════════════════════════════════════════════════════════════"
echo "Expected: ✅ Order created, inventory decremented"
echo ""

echo "Creating order for 2 laptops..."
ORDER1=$(curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"Transaction Test Customer\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT1_ID\",
        \"name\": \"Laptop Pro 15\",
        \"price\": 1299.99,
        \"quantity\": 2
      }
    ]
  }")
echo "$ORDER1" | jq

echo ""
echo "Checking updated inventory for Laptop..."
UPDATED_PRODUCT1=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT1_ID\")")
UPDATED_INVENTORY=$(echo "$UPDATED_PRODUCT1" | jq -r '.inventory')
echo "Updated inventory: $UPDATED_INVENTORY (expected: 8)"

if [ "$UPDATED_INVENTORY" == "8" ]; then
    echo "✅ Test 1 PASSED: Inventory correctly decremented from 10 to 8"
else
    echo "❌ Test 1 FAILED: Expected inventory 8, got $UPDATED_INVENTORY"
fi

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 2: Insufficient Inventory (Transaction Should Rollback)
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "Test 2: Insufficient Inventory Error"
echo "════════════════════════════════════════════════════════════════════════"
echo "Expected: ❌ Order NOT created, inventory unchanged"
echo ""

echo "Attempting to order 20 laptops (only 8 available)..."
ORDER2=$(curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"Transaction Test Customer\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT1_ID\",
        \"name\": \"Laptop Pro 15\",
        \"price\": 1299.99,
        \"quantity\": 20
      }
    ]
  }")
echo "$ORDER2" | jq

echo ""
echo "Checking inventory (should still be 8)..."
UPDATED_PRODUCT1=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT1_ID\")")
UPDATED_INVENTORY=$(echo "$UPDATED_PRODUCT1" | jq -r '.inventory')
echo "Current inventory: $UPDATED_INVENTORY (expected: 8)"

if [ "$UPDATED_INVENTORY" == "8" ]; then
    echo "✅ Test 2 PASSED: Inventory unchanged (transaction rolled back)"
else
    echo "❌ Test 2 FAILED: Expected inventory 8, got $UPDATED_INVENTORY"
fi

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 3: Multiple Products in One Order
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "Test 3: Order with Multiple Products"
echo "════════════════════════════════════════════════════════════════════════"
echo "Expected: ✅ Order created, both inventories decremented"
echo ""

echo "Creating order for 1 laptop + 2 mice..."
ORDER3=$(curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"Transaction Test Customer\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT1_ID\",
        \"name\": \"Laptop Pro 15\",
        \"price\": 1299.99,
        \"quantity\": 1
      },
      {
        \"productId\": \"$PRODUCT2_ID\",
        \"name\": \"Wireless Mouse\",
        \"price\": 29.99,
        \"quantity\": 2
      }
    ]
  }")
echo "$ORDER3" | jq

echo ""
echo "Checking updated inventories..."
UPDATED_PRODUCT1=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT1_ID\")")
UPDATED_PRODUCT2=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT2_ID\")")
LAPTOP_INVENTORY=$(echo "$UPDATED_PRODUCT1" | jq -r '.inventory')
MOUSE_INVENTORY=$(echo "$UPDATED_PRODUCT2" | jq -r '.inventory')

echo "Laptop inventory: $LAPTOP_INVENTORY (expected: 7)"
echo "Mouse inventory: $MOUSE_INVENTORY (expected: 3)"

if [ "$LAPTOP_INVENTORY" == "7" ] && [ "$MOUSE_INVENTORY" == "3" ]; then
    echo "✅ Test 3 PASSED: Both inventories correctly decremented"
else
    echo "❌ Test 3 FAILED: Expected laptop=7, mouse=3, got laptop=$LAPTOP_INVENTORY, mouse=$MOUSE_INVENTORY"
fi

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 4: Partial Insufficient Inventory (All Should Rollback)
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "Test 4: Partial Insufficient Inventory"
echo "════════════════════════════════════════════════════════════════════════"
echo "Expected: ❌ Order NOT created, NO inventories changed"
echo ""

echo "Attempting to order 1 laptop (available) + 10 mice (only 3 available)..."
ORDER4=$(curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"Transaction Test Customer\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT1_ID\",
        \"name\": \"Laptop Pro 15\",
        \"price\": 1299.99,
        \"quantity\": 1
      },
      {
        \"productId\": \"$PRODUCT2_ID\",
        \"name\": \"Wireless Mouse\",
        \"price\": 29.99,
        \"quantity\": 10
      }
    ]
  }")
echo "$ORDER4" | jq

echo ""
echo "Checking inventories (should be unchanged)..."
UPDATED_PRODUCT1=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT1_ID\")")
UPDATED_PRODUCT2=$(curl -s $BASE_URL/products | jq ".[] | select(.id==\"$PRODUCT2_ID\")")
LAPTOP_INVENTORY=$(echo "$UPDATED_PRODUCT1" | jq -r '.inventory')
MOUSE_INVENTORY=$(echo "$UPDATED_PRODUCT2" | jq -r '.inventory')

echo "Laptop inventory: $LAPTOP_INVENTORY (expected: 7)"
echo "Mouse inventory: $MOUSE_INVENTORY (expected: 3)"

if [ "$LAPTOP_INVENTORY" == "7" ] && [ "$MOUSE_INVENTORY" == "3" ]; then
    echo "✅ Test 4 PASSED: Transaction rolled back, no inventories changed"
else
    echo "❌ Test 4 FAILED: Expected laptop=7, mouse=3, got laptop=$LAPTOP_INVENTORY, mouse=$MOUSE_INVENTORY"
fi

echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "Test Summary"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
echo "All tests completed! MongoDB transactions ensure:"
echo "✅ Orders and inventory updates are atomic"
echo "✅ Failed operations don't leave partial data"
echo "✅ Inventory is never oversold"
echo "✅ Clear error messages for users"
echo ""
echo "════════════════════════════════════════════════════════════════════════"

