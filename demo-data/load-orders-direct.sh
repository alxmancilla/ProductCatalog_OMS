#!/bin/bash

###############################################################################
# Load Orders Directly into MongoDB
#
# This script loads historical orders directly into MongoDB using mongoimport.
# This approach is used because:
# 1. The API creates all orders as PENDING with today's date
# 2. We want to preserve 2 years of historical data
# 3. We want to preserve complete status histories
# 4. This is faster (5000 orders in seconds vs. minutes)
#
# Prerequisites:
# - Customers and Products must be loaded first (to get real IDs)
# - orders-final.json must exist (created by load-demo-data.sh)
###############################################################################

set -e

DB_NAME="product_catalog"
ORDERS_FILE="demo-data/orders-final.json"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Load Historical Orders - Direct MongoDB Insert               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if mongoimport exists
if ! command -v mongoimport &> /dev/null; then
    echo -e "${RED}❌ mongoimport not found${NC}"
    echo -e "${YELLOW}   Please install MongoDB Database Tools:${NC}"
    echo "   https://www.mongodb.com/docs/database-tools/installation/"
    exit 1
fi

# Check if orders file exists
if [ ! -f "$ORDERS_FILE" ]; then
    echo -e "${RED}❌ Orders file not found: $ORDERS_FILE${NC}"
    echo -e "${YELLOW}   Please run the full loading script first:${NC}"
    echo "   ./demo-data/load-demo-data.sh"
    exit 1
fi

# Check MongoDB is running
if ! mongosh --eval "db.version()" > /dev/null 2>&1; then
    echo -e "${RED}❌ MongoDB is not running${NC}"
    echo -e "${YELLOW}   Please start MongoDB first${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites met${NC}"
echo ""

# Count orders in file
ORDER_COUNT=$(jq 'length' "$ORDERS_FILE")
echo -e "${BLUE}📊 Orders to load:${NC} $ORDER_COUNT"
echo ""

# Import orders
echo -e "${YELLOW}🛒 Importing orders into MongoDB...${NC}"
echo ""

mongoimport \
    --db "$DB_NAME" \
    --collection orders \
    --file "$ORDERS_FILE" \
    --jsonArray \
    --drop

echo ""
echo -e "${GREEN}✅ Orders imported successfully${NC}"
echo ""

# Verify import
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Verification${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Count documents
IMPORTED_COUNT=$(mongosh "$DB_NAME" --quiet --eval "db.orders.countDocuments()")
echo -e "Total Orders: ${GREEN}$IMPORTED_COUNT${NC}"

# Status distribution
echo ""
echo "Status Distribution:"
mongosh "$DB_NAME" --quiet --eval '
db.orders.aggregate([
  { $group: { _id: "$status", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
]).forEach(doc => print("  " + doc._id + ": " + doc.count))
'

# Date range
echo ""
echo "Date Range:"
mongosh "$DB_NAME" --quiet --eval '
const result = db.orders.aggregate([
  { $group: { 
      _id: null,
      minDate: { $min: "$orderDate" },
      maxDate: { $max: "$orderDate" }
  }}
]).toArray()[0];
if (result) {
  print("  From: " + result.minDate);
  print("  To:   " + result.maxDate);
}
'

# Sample order
echo ""
echo "Sample Order:"
mongosh "$DB_NAME" --quiet --eval '
const order = db.orders.findOne();
if (order) {
  print("  ID: " + order._id);
  print("  Customer: " + order.customerName);
  print("  Status: " + order.status);
  print("  Total: $" + order.total);
  print("  Items: " + order.items.length);
  print("  Status Changes: " + order.statusHistory.length);
}
'

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 Historical orders loaded successfully!${NC}"
echo ""
echo -e "${YELLOW}📊 You can now:${NC}"
echo "   1. View orders via API: curl http://localhost:8080/orders | jq"
echo "   2. Run analytics: curl http://localhost:8080/analytics/orders/revenue-by-status | jq"
echo "   3. Search orders: curl 'http://localhost:8080/orders?status=DELIVERED' | jq"
echo ""
echo -e "${BLUE}💡 Note:${NC} Inventory was NOT decremented (orders are historical)"
echo "   This is normal for demo purposes - shows how MongoDB handles bulk imports!"
echo ""
