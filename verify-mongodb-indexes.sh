#!/bin/bash
# ============================================================================
# MongoDB Indexes Verification Script
# ============================================================================
# This script verifies that all required MongoDB indexes for P0 OMS features
# are properly created and that queries use them efficiently.
#
# Prerequisites:
# - MongoDB running
# - mongosh CLI installed
#
# Usage: ./verify-mongodb-indexes.sh
# ============================================================================

DB_NAME="product_catalog_oms"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "════════════════════════════════════════════════════════════════════════"
echo "🔍 MongoDB Indexes Verification"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
echo "Database: $DB_NAME"
echo ""

# ============================================================================
# Check if MongoDB is running
# ============================================================================
if ! mongosh --quiet --eval "db.version()" > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: MongoDB is not running or mongosh is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ MongoDB connection successful${NC}"
echo ""

# ============================================================================
# Function to check if index exists
# ============================================================================
check_index() {
    local collection=$1
    local index_name=$2
    local index_keys=$3
    
    echo -e "${BLUE}Checking index: $index_name on $collection${NC}"
    
    # Get index info
    local index_info=$(mongosh --quiet --eval "
        use $DB_NAME;
        db.$collection.getIndexes().find(idx => idx.name === '$index_name');
    " 2>/dev/null)
    
    if [ -z "$index_info" ] || [ "$index_info" = "undefined" ]; then
        echo -e "${RED}  ❌ Index '$index_name' NOT FOUND${NC}"
        echo -e "${YELLOW}  Create it with: db.$collection.createIndex($index_keys)${NC}"
        return 1
    else
        echo -e "${GREEN}  ✓ Index '$index_name' exists${NC}"
        return 0
    fi
}

# ============================================================================
# Check Orders Collection Indexes
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "📦 Orders Collection Indexes"
echo "════════════════════════════════════════════════════════════════════════"
echo ""

# List all existing indexes on orders collection
echo -e "${BLUE}Current indexes on 'orders' collection:${NC}"
mongosh --quiet --eval "
    use $DB_NAME;
    db.orders.getIndexes().forEach(idx => {
        print('  - ' + idx.name + ': ' + JSON.stringify(idx.key));
    });
" 2>/dev/null
echo ""

# Check required indexes
echo -e "${BLUE}Verifying required indexes:${NC}"
echo ""

# Index 1: Status + OrderDate (for status filtering)
check_index "orders" "status_1_orderDate_-1" "{ status: 1, orderDate: -1 }"
echo ""

# Index 2: CustomerId + OrderDate (for customer queries)
check_index "orders" "customerId_1_orderDate_-1" "{ customerId: 1, orderDate: -1 }"
echo ""

# Index 3: CustomerId + Status + OrderDate (compound for customer+status queries)
check_index "orders" "customerId_1_status_1_orderDate_-1" "{ customerId: 1, status: 1, orderDate: -1 }"
echo ""

# Index 4: OrderDate (for date range queries)
check_index "orders" "orderDate_-1" "{ orderDate: -1 }"
echo ""

# Index 5: Total (for price range queries)
check_index "orders" "total_1" "{ total: 1 }"
echo ""

# ============================================================================
# Check Order Item Buckets Collection Indexes
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "📦 Order Item Buckets Collection Indexes"
echo "════════════════════════════════════════════════════════════════════════"
echo ""

# List all existing indexes on order_item_buckets collection
echo -e "${BLUE}Current indexes on 'order_item_buckets' collection:${NC}"
mongosh --quiet --eval "
    use $DB_NAME;
    db.order_item_buckets.getIndexes().forEach(idx => {
        print('  - ' + idx.name + ': ' + JSON.stringify(idx.key));
    });
" 2>/dev/null
echo ""

# Check required indexes
echo -e "${BLUE}Verifying required indexes:${NC}"
echo ""

# Index: OrderId + BucketNumber (for large order retrieval)
check_index "order_item_buckets" "orderId_1_bucketNumber_1" "{ orderId: 1, bucketNumber: 1 }"
echo ""

# ============================================================================
# Test Query Performance
# ============================================================================
echo "════════════════════════════════════════════════════════════════════════"
echo "⚡ Query Performance Test"
echo "════════════════════════════════════════════════════════════════════════"
echo ""

echo -e "${BLUE}Testing query plans to ensure indexes are used...${NC}"
echo ""

# Test 1: Query by status
echo -e "${BLUE}Test 1: Query by status${NC}"
mongosh --quiet --eval "
    use $DB_NAME;
    const explain = db.orders.find({ status: 'PENDING' }).sort({ orderDate: -1 }).explain('executionStats');
    const stage = explain.executionStats.executionStages;
    if (stage.stage === 'IXSCAN' || (stage.inputStage && stage.inputStage.stage === 'IXSCAN')) {
        print('  ✓ Uses index: ' + (stage.indexName || stage.inputStage.indexName));
    } else {
        print('  ❌ COLLSCAN detected - index not used!');
    }
" 2>/dev/null
echo ""

# Test 2: Query by customer
echo -e "${BLUE}Test 2: Query by customerId${NC}"
mongosh --quiet --eval "
    use $DB_NAME;
    const explain = db.orders.find({ customerId: 'test123' }).sort({ orderDate: -1 }).explain('executionStats');
    const stage = explain.executionStats.executionStages;
    if (stage.stage === 'IXSCAN' || (stage.inputStage && stage.inputStage.stage === 'IXSCAN')) {
        print('  ✓ Uses index: ' + (stage.indexName || stage.inputStage.indexName));
    } else {
        print('  ❌ COLLSCAN detected - index not used!');
    }
" 2>/dev/null
echo ""

# Test 3: Query by customer and status
echo -e "${BLUE}Test 3: Query by customerId + status${NC}"
mongosh --quiet --eval "
    use $DB_NAME;
    const explain = db.orders.find({ customerId: 'test123', status: 'PENDING' }).explain('executionStats');
    const stage = explain.executionStats.executionStages;
    if (stage.stage === 'IXSCAN' || (stage.inputStage && stage.inputStage.stage === 'IXSCAN')) {
        print('  ✓ Uses index: ' + (stage.indexName || stage.inputStage.indexName));
    } else {
        print('  ❌ COLLSCAN detected - index not used!');
    }
" 2>/dev/null
echo ""

echo "════════════════════════════════════════════════════════════════════════"
echo "✅ Index Verification Complete!"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
echo "Summary:"
echo "- All required indexes should be created by OrderIndexConfiguration.java"
echo "- If indexes are missing, restart the application to trigger creation"
echo "- Query plans should show 'IXSCAN' (index scan) not 'COLLSCAN' (collection scan)"
echo ""

