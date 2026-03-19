#!/bin/bash

# ============================================================================
# MongoDB Schema Validation Test Script
# ============================================================================
# This script demonstrates MongoDB's schema validation by attempting to
# insert both valid and invalid documents.
#
# Usage: ./test-schema-validation.sh
# ============================================================================

BASE_URL="http://localhost:8080"

echo "============================================================================"
echo "🛡️  MongoDB Schema Validation Test Suite"
echo "============================================================================"
echo ""
echo "This script tests both Spring Boot validation and MongoDB schema validation."
echo "It demonstrates defense-in-depth data quality enforcement."
echo ""

# ============================================================================
# Test 1: Valid Product (Should Succeed)
# ============================================================================
echo "============================================================================"
echo "Test 1: Valid Product (All Required Fields)"
echo "============================================================================"
echo "Expected: ✅ Success (201 Created)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product Valid",
    "description": "This is a valid test product with all required fields",
    "price": 99.99,
    "category": "Test",
    "inventory": 10,
    "sku": "TEST-PROD-VALID"
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 2: Missing Description (Spring Boot Validation Catches)
# ============================================================================
echo "============================================================================"
echo "Test 2: Missing Description Field"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error (400 Bad Request)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product No Description",
    "price": 99.99,
    "category": "Test",
    "inventory": 10
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 3: Empty Name (Spring Boot Validation Catches)
# ============================================================================
echo "============================================================================"
echo "Test 3: Empty Name Field"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error (400 Bad Request)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "description": "Product with empty name",
    "price": 99.99,
    "category": "Test",
    "inventory": 10
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 4: Negative Price (Spring Boot Validation Catches)
# ============================================================================
echo "============================================================================"
echo "Test 4: Negative Price"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error (400 Bad Request)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product Negative Price",
    "description": "Product with negative price",
    "price": -10.00,
    "category": "Test",
    "inventory": 10
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 5: Negative Inventory (Spring Boot Validation Catches)
# ============================================================================
echo "============================================================================"
echo "Test 5: Negative Inventory"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error (400 Bad Request)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product Negative Inventory",
    "description": "Product with negative inventory",
    "price": 99.99,
    "category": "Test",
    "inventory": -5
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 6: Multiple Validation Errors
# ============================================================================
echo "============================================================================"
echo "Test 6: Multiple Validation Errors"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error with multiple field errors"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "price": -10.00,
    "inventory": -5
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 7: Missing SKU (Spring Boot Validation Catches)
# ============================================================================
echo "============================================================================"
echo "Test 7: Missing SKU Field"
echo "============================================================================"
echo "Expected: ❌ Spring Boot validation error (400 Bad Request)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product No SKU",
    "description": "Product without SKU field",
    "price": 99.99,
    "category": "Test",
    "inventory": 10
  }' | jq

echo ""
read -p "Press Enter to continue..."
echo ""

# ============================================================================
# Test 8: Valid Customer (Should Succeed)
# ============================================================================
echo "============================================================================"
echo "Test 8: Valid Customer"
echo "============================================================================"
echo "Expected: ✅ Success (201 Created)"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Customer",
    "email": "test@example.com",
    "phone": "+1-555-0123"
  }' | jq

echo ""
echo "============================================================================"
echo "✅ Schema Validation Test Suite Complete!"
echo "============================================================================"
echo ""
echo "Summary:"
echo "- Spring Boot validation (@Valid) catches errors at the API layer"
echo "- MongoDB schema validation provides database-level protection"
echo "- Together they provide defense-in-depth data quality enforcement"
echo ""
echo "To test MongoDB schema validation directly (bypassing Spring Boot):"
echo "1. Open mongosh"
echo "2. Run: use product_catalog_oms"
echo "3. Try: db.products.insertOne({name: '', price: -10})"
echo "4. MongoDB will reject it with a validation error!"
echo ""

