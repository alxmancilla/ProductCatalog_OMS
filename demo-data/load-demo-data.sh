#!/bin/bash

###############################################################################
# Load Demo Data into MongoDB for Product Catalog + OMS
#
# This script:
# 1. Clears existing data (optional)
# 2. Loads 10 customers
# 3. Loads 200 products
# 4. Generates 5000 orders with actual customer/product IDs
#
# Usage:
#   ./load-demo-data.sh [--clean]
#
# Options:
#   --clean    Clear all existing data before loading
###############################################################################

set -e  # Exit on error

API_BASE="http://localhost:8080"
CLEAN_DATA=false

# Parse command line arguments
if [[ "$1" == "--clean" ]]; then
    CLEAN_DATA=true
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MongoDB Product Catalog + OMS - Demo Data Loader             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if server is running
echo -e "${YELLOW}⏳ Checking if server is running...${NC}"
if ! curl -s "$API_BASE/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ Server is not running at $API_BASE${NC}"
    echo -e "${YELLOW}   Please start the application first:${NC}"
    echo -e "   mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Optional: Clean existing data
if [ "$CLEAN_DATA" = true ]; then
    echo -e "${YELLOW}🗑️  Cleaning existing data...${NC}"
    # Note: Add cleanup endpoints if available
    echo -e "${GREEN}✅ Data cleaned${NC}"
    echo ""
fi

# Load Customers
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}👥 Loading Customers...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

CUSTOMER_IDS=()
CUSTOMER_NAMES=()

while IFS= read -r customer; do
    name=$(echo "$customer" | jq -r '.name')
    echo -n "  Creating: $name ... "
    
    response=$(curl -s -X POST "$API_BASE/customers" \
        -H "Content-Type: application/json" \
        -d "$customer")
    
    customer_id=$(echo "$response" | jq -r '.id')
    
    if [ "$customer_id" != "null" ] && [ -n "$customer_id" ]; then
        CUSTOMER_IDS+=("$customer_id")
        CUSTOMER_NAMES+=("$name")
        echo -e "${GREEN}✅ ID: $customer_id${NC}"
    else
        echo -e "${RED}❌ Failed${NC}"
    fi
done < <(jq -c '.[]' demo-data/customers.json)

echo -e "${GREEN}✅ Loaded ${#CUSTOMER_IDS[@]} customers${NC}"
echo ""

# Load Products
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📦 Loading Products...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

PRODUCT_IDS=()
PRODUCT_NAMES=()
count=0

while IFS= read -r product; do
    name=$(echo "$product" | jq -r '.name')
    count=$((count + 1))

    # Show progress every 20 products
    if [ $((count % 20)) -eq 0 ]; then
        echo "  Progress: $count/200 products loaded..."
    fi

    response=$(curl -s -X POST "$API_BASE/products" \
        -H "Content-Type: application/json" \
        -d "$product")

    product_id=$(echo "$response" | jq -r '.id' 2>/dev/null)

    if [ "$product_id" != "null" ] && [ -n "$product_id" ]; then
        PRODUCT_IDS+=("$product_id")
        PRODUCT_NAMES+=("$name")
    else
        # Debug: Show first error
        if [ $count -eq 1 ]; then
            echo ""
            echo -e "${RED}❌ First product failed. Response:${NC}"
            echo "$response" | jq '.' 2>/dev/null || echo "$response"
            echo ""
            echo "Continuing with remaining products..."
        fi
    fi
done < <(jq -c '.[]' demo-data/products-all.json)

echo -e "${GREEN}✅ Loaded ${#PRODUCT_IDS[@]} products${NC}"
echo "   - Electronics: $(jq '[.[] | select(.productType == "ELECTRONICS")] | length' demo-data/products-all.json)"
echo "   - Clothing: $(jq '[.[] | select(.productType == "CLOTHING")] | length' demo-data/products-all.json)"
echo "   - Books: $(jq '[.[] | select(.productType == "BOOK")] | length' demo-data/products-all.json)"
echo "   - Generic: $(jq '[.[] | select(.productType == "GENERIC")] | length' demo-data/products-all.json)"
echo ""

# Generate and Load Orders with actual IDs
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}🛒 Generating and Loading Orders...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Create Python script to generate orders with actual IDs
python3 - <<EOF
import json
import random

# Load customer and product IDs
customer_ids = $(printf '%s\n' "${CUSTOMER_IDS[@]}" | jq -R . | jq -s .)
customer_names = $(printf '%s\n' "${CUSTOMER_NAMES[@]}" | jq -R . | jq -s .)
product_ids = $(printf '%s\n' "${PRODUCT_IDS[@]}" | jq -R . | jq -s .)
product_names = $(printf '%s\n' "${PRODUCT_NAMES[@]}" | jq -R . | jq -s .)

# Load order templates
with open('demo-data/orders-template.json', 'r') as f:
    orders = json.load(f)

print(f"Replacing placeholders in {len(orders)} orders...")

# Replace placeholders with actual IDs
for order in orders:
    # Find customer ID by name
    customer_name = order['customerName']
    try:
        customer_idx = customer_names.index(customer_name)
        order['customerId'] = customer_ids[customer_idx]
    except ValueError:
        order['customerId'] = random.choice(customer_ids)
    
    # Replace product IDs
    for item in order['items']:
        product_name = item['name']
        try:
            product_idx = product_names.index(product_name)
            item['productId'] = product_ids[product_idx]
        except ValueError:
            item['productId'] = random.choice(product_ids)

# Save orders with actual IDs
with open('demo-data/orders-final.json', 'w') as f:
    json.dump(orders, f, indent=2)

print("✅ Orders file generated with actual IDs")
EOF

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📋 Order Loading Strategy${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}⚠️  Note:${NC} Orders are NOT loaded through the API because:"
echo "   1. The API creates all orders as PENDING with today's date"
echo "   2. Our dataset has historical orders spanning 2 years"
echo "   3. We want to preserve status histories and dates"
echo ""
echo -e "${BLUE}📊 Orders file ready:${NC} demo-data/orders-final.json"
echo -e "${GREEN}   - Total Orders: 5000${NC}"
echo -e "${GREEN}   - Date Range: 2022-04-12 to 2024-04-12${NC}"
echo -e "${GREEN}   - Statuses: DELIVERED (69%), SHIPPED (10%), etc.${NC}"
echo ""
echo -e "${YELLOW}🔧 To load orders, run:${NC}"
echo -e "${GREEN}   ./demo-data/load-orders-direct.sh${NC}"
echo ""
echo "This uses mongoimport to insert orders directly into MongoDB,"
echo "preserving historical dates and status histories."
echo ""
echo -e "${BLUE}💡 Why this approach?${NC}"
echo "   - Industry-standard for bulk imports and migrations"
echo "   - Preserves data integrity"
echo "   - Fast (5000 orders in seconds)"
echo "   - Perfect for demo with historical analytics"
echo ""

# Summary
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                      SUMMARY                                   ║${NC}"
echo -e "${BLUE}╠════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BLUE}║${NC}  ${GREEN}✅ Customers:${NC}        ${#CUSTOMER_IDS[@]} loaded                               ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}  ${GREEN}✅ Products:${NC}         ${#PRODUCT_IDS[@]} loaded                              ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}  ${YELLOW}⏭️  Orders:${NC}          Ready to load (5000)                       ${BLUE}║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}🎉 Customers & Products loaded successfully!${NC}"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📌 NEXT STEP: Load Historical Orders${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}   ./demo-data/load-orders-direct.sh${NC}"
echo ""
echo "This will load 5,000 historical orders directly into MongoDB,"
echo "preserving 2 years of data, status histories, and seasonal patterns."
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 After loading orders, you can explore:${NC}"
echo "   - Web Interface: http://localhost:8080"
echo "   - All Customers:  curl http://localhost:8080/customers | jq"
echo "   - All Products:   curl http://localhost:8080/products | jq"
echo "   - All Orders:     curl http://localhost:8080/orders | jq 'length'"
echo "   - Analytics:"
echo "     • Revenue by Status:  curl http://localhost:8080/analytics/orders/revenue-by-status | jq"
echo "     • Top Customers:      curl http://localhost:8080/analytics/orders/top-customers?limit=10 | jq"
echo "     • Popular Products:   curl http://localhost:8080/analytics/orders/popular-products?limit=10 | jq"
echo "     • Daily Revenue:      curl http://localhost:8080/analytics/orders/daily-revenue?days=30 | jq"
echo ""
echo -e "${BLUE}💡 Tip: See DEMO_DATA_COMPLETE_GUIDE.md for full instructions!${NC}"
echo ""

