#!/bin/bash

# Product Catalog + OMS Demo Commands
# This script contains all the curl commands for the webinar demo

echo "========================================="
echo "Product Catalog + OMS Demo"
echo "========================================="
echo ""

# Base URL
BASE_URL="http://localhost:8080"

echo "1. Creating a Customer..."
echo "------------------------"

# Create Customer
echo "Creating customer John Doe..."
CUSTOMER=$(curl -s -X POST $BASE_URL/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123"
  }')
echo "$CUSTOMER" | jq
CUSTOMER_ID=$(echo "$CUSTOMER" | jq -r '.id')
echo "Customer ID: $CUSTOMER_ID"
echo ""

echo "2. Creating Products..."
echo "------------------------"

# Create Product 1: Laptop
echo "Creating Laptop Pro 15..."
PRODUCT1=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50
  }')
echo "$PRODUCT1" | jq
PRODUCT1_ID=$(echo "$PRODUCT1" | jq -r '.id')
echo "Product 1 ID: $PRODUCT1_ID"
echo ""

# Create Product 2: Wireless Mouse
echo "Creating Wireless Mouse..."
PRODUCT2=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "price": 29.99,
    "category": "Accessories",
    "inventory": 200
  }')
echo "$PRODUCT2" | jq
PRODUCT2_ID=$(echo "$PRODUCT2" | jq -r '.id')
echo "Product 2 ID: $PRODUCT2_ID"
echo ""

# Create Product 3: USB-C Cable
echo "Creating USB-C Cable..."
PRODUCT3=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "USB-C Cable 2m",
    "price": 15.99,
    "category": "Accessories",
    "inventory": 500
  }')
echo "$PRODUCT3" | jq
PRODUCT3_ID=$(echo "$PRODUCT3" | jq -r '.id')
echo "Product 3 ID: $PRODUCT3_ID"
echo ""

echo "3. Getting All Products..."
echo "------------------------"
curl -s $BASE_URL/products | jq
echo ""

echo "4. Creating an Order..."
echo "------------------------"
ORDER=$(curl -s -X POST $BASE_URL/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"customerName\": \"John Doe\",
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
echo "$ORDER" | jq
echo ""

echo "5. Getting All Orders..."
echo "------------------------"
curl -s $BASE_URL/orders | jq
echo ""

echo "========================================="
echo "Demo Complete!"
echo "========================================="
echo ""
echo "MongoDB Shell Commands:"
echo "  mongosh"
echo "  use sample_pc_oms"
echo "  db.customers.find().pretty()"
echo "  db.products.find().pretty()"
echo "  db.orders.find().pretty()"
echo ""

