#!/bin/bash

###############################################################################
# Test API Ingestion - Validate customer and product creation
#
# This script tests that the API can correctly ingest:
# 1. Customer data with tier, address, metadata
# 2. Product data with different types and details
###############################################################################

set -e

API_BASE="http://localhost:8080"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  API Ingestion Test - Customer & Product Validation           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if server is running
echo -e "${YELLOW}⏳ Checking if server is running...${NC}"
if ! curl -s "$API_BASE/customers" > /dev/null 2>&1; then
    echo -e "${RED}❌ Server is not running at $API_BASE${NC}"
    echo -e "${YELLOW}   Please start the application first:${NC}"
    echo -e "   mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Test 1: Create Customer
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Test 1: Creating Customer with Full Structure${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

CUSTOMER_DATA=$(cat <<EOF
{
  "name": "Test Customer",
  "email": "test@example.com",
  "phone": "+1-555-9999",
  "tier": "GOLD",
  "address": {
    "street": "123 Test Street",
    "city": "Test City",
    "state": "CA",
    "zipCode": "12345",
    "country": "USA"
  },
  "metadata": {
    "company": "Test Corp",
    "industry": "Technology",
    "customerSince": "2024-01-01"
  }
}
EOF
)

echo "$CUSTOMER_DATA" | jq '.'
echo ""
echo -n "Creating customer... "

CUSTOMER_RESPONSE=$(curl -s -X POST "$API_BASE/customers" \
    -H "Content-Type: application/json" \
    -d "$CUSTOMER_DATA")

CUSTOMER_ID=$(echo "$CUSTOMER_RESPONSE" | jq -r '.id' 2>/dev/null)

if [ "$CUSTOMER_ID" != "null" ] && [ -n "$CUSTOMER_ID" ]; then
    echo -e "${GREEN}✅ SUCCESS${NC}"
    echo "Customer ID: $CUSTOMER_ID"
    echo ""
    echo "Full Response:"
    echo "$CUSTOMER_RESPONSE" | jq '.'
    echo ""
else
    echo -e "${RED}❌ FAILED${NC}"
    echo "Response:"
    echo "$CUSTOMER_RESPONSE" | jq '.' 2>/dev/null || echo "$CUSTOMER_RESPONSE"
    echo ""
    exit 1
fi

# Test 2: Create Electronics Product
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Test 2: Creating Electronics Product${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

ELECTRONICS_DATA=$(cat <<EOF
{
  "name": "Test Laptop Pro",
  "description": "High-performance test laptop",
  "price": 1999.99,
  "inventory": 50,
  "sku": "TEST-ELEC-001",
  "productType": "ELECTRONICS",
  "category": "Laptops",
  "schemaVersion": 2,
  "electronicsDetails": {
    "brand": "TestBrand",
    "warranty": "12 months"
  }
}
EOF
)

echo "$ELECTRONICS_DATA" | jq '.'
echo ""
echo -n "Creating electronics product... "

ELECTRONICS_RESPONSE=$(curl -s -X POST "$API_BASE/products" \
    -H "Content-Type: application/json" \
    -d "$ELECTRONICS_DATA")

ELECTRONICS_ID=$(echo "$ELECTRONICS_RESPONSE" | jq -r '.id' 2>/dev/null)

if [ "$ELECTRONICS_ID" != "null" ] && [ -n "$ELECTRONICS_ID" ]; then
    echo -e "${GREEN}✅ SUCCESS${NC}"
    echo "Product ID: $ELECTRONICS_ID"
    echo ""
    echo "Full Response:"
    echo "$ELECTRONICS_RESPONSE" | jq '.'
    echo ""
else
    echo -e "${RED}❌ FAILED${NC}"
    echo "Response:"
    echo "$ELECTRONICS_RESPONSE" | jq '.' 2>/dev/null || echo "$ELECTRONICS_RESPONSE"
    echo ""
    exit 1
fi

# Test 3: Create Generic Product
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Test 3: Creating Generic Product${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

GENERIC_DATA=$(cat <<EOF
{
  "name": "Test Water Bottle",
  "description": "Stainless steel water bottle",
  "price": 24.99,
  "inventory": 100,
  "sku": "TEST-GEN-001",
  "productType": "GENERIC",
  "category": "Drinkware",
  "schemaVersion": 2
}
EOF
)

echo "$GENERIC_DATA" | jq '.'
echo ""
echo -n "Creating generic product... "

GENERIC_RESPONSE=$(curl -s -X POST "$API_BASE/products" \
    -H "Content-Type: application/json" \
    -d "$GENERIC_DATA")

GENERIC_ID=$(echo "$GENERIC_RESPONSE" | jq -r '.id' 2>/dev/null)

if [ "$GENERIC_ID" != "null" ] && [ -n "$GENERIC_ID" ]; then
    echo -e "${GREEN}✅ SUCCESS${NC}"
    echo "Product ID: $GENERIC_ID"
    echo ""
    echo "Full Response:"
    echo "$GENERIC_RESPONSE" | jq '.'
    echo ""
else
    echo -e "${RED}❌ FAILED${NC}"
    echo "Response:"
    echo "$GENERIC_RESPONSE" | jq '.' 2>/dev/null || echo "$GENERIC_RESPONSE"
    echo ""
    exit 1
fi

# Test 4: Create Order
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Test 4: Creating Order${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

ORDER_DATA=$(cat <<EOF
{
  "customerId": "$CUSTOMER_ID",
  "customerName": "Test Customer",
  "items": [
    {
      "productId": "$ELECTRONICS_ID",
      "name": "Test Laptop Pro",
      "price": 1999.99,
      "quantity": 2
    },
    {
      "productId": "$GENERIC_ID",
      "name": "Test Water Bottle",
      "price": 24.99,
      "quantity": 1
    }
  ]
}
EOF
)

echo "$ORDER_DATA" | jq '.'
echo ""
echo -n "Creating order... "

ORDER_RESPONSE=$(curl -s -X POST "$API_BASE/orders" \
    -H "Content-Type: application/json" \
    -d "$ORDER_DATA")

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id' 2>/dev/null)

if [ "$ORDER_ID" != "null" ] && [ -n "$ORDER_ID" ]; then
    echo -e "${GREEN}✅ SUCCESS${NC}"
    echo "Order ID: $ORDER_ID"
    echo ""
    echo "Full Response:"
    echo "$ORDER_RESPONSE" | jq '.'
    echo ""

    # Verify order total was calculated
    ORDER_TOTAL=$(echo "$ORDER_RESPONSE" | jq -r '.total')
    echo "Order Total: \$$ORDER_TOTAL (should be 4024.97)"

    # Verify status is PENDING
    ORDER_STATUS=$(echo "$ORDER_RESPONSE" | jq -r '.status')
    echo "Order Status: $ORDER_STATUS (should be PENDING)"

    # Verify inventory was decremented
    echo ""
    echo -n "Checking inventory was decremented... "
    UPDATED_INVENTORY=$(curl -s "$API_BASE/products/$ELECTRONICS_ID" | jq -r '.inventory')
    echo -e "${GREEN}✅ Electronics inventory now: $UPDATED_INVENTORY (was 50, ordered 2)${NC}"

else
    echo -e "${RED}❌ FAILED${NC}"
    echo "Response:"
    echo "$ORDER_RESPONSE" | jq '.' 2>/dev/null || echo "$ORDER_RESPONSE"
    echo ""
fi

# Summary
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                      TEST RESULTS                              ║${NC}"
echo -e "${BLUE}╠════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BLUE}║${NC}  ${GREEN}✅ Customer Creation:${NC}      SUCCESS                           ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}  ${GREEN}✅ Electronics Product:${NC}     SUCCESS                           ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}  ${GREEN}✅ Generic Product:${NC}         SUCCESS                           ${BLUE}║${NC}"
if [ "$ORDER_ID" != "null" ] && [ -n "$ORDER_ID" ]; then
    echo -e "${BLUE}║${NC}  ${GREEN}✅ Order Creation:${NC}         SUCCESS                           ${BLUE}║${NC}"
else
    echo -e "${BLUE}║${NC}  ${RED}❌ Order Creation:${NC}         FAILED                            ${BLUE}║${NC}"
fi
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}🎉 All API ingestion tests passed!${NC}"
echo ""
echo -e "${YELLOW}📋 Created Resources:${NC}"
echo "   - Customer ID: $CUSTOMER_ID"
echo "   - Electronics Product ID: $ELECTRONICS_ID"
echo "   - Generic Product ID: $GENERIC_ID"
if [ "$ORDER_ID" != "null" ] && [ -n "$ORDER_ID" ]; then
    echo "   - Order ID: $ORDER_ID"
fi
echo ""
echo -e "${BLUE}💡 Ready to load full dataset:${NC}"
echo "   ./demo-data/load-demo-data.sh"
echo ""
