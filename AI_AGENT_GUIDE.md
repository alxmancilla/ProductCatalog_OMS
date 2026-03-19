# AI Order Assistant Guide

## 🤖 Natural Language Order Creation with LangGraph + MongoDB

This guide explains the **AI Order Assistant** - a natural language interface for creating orders using **LangGraph** state machines, **Grove API** (OpenAI GPT-5.4), and **MongoDB Atlas**.

---

## 🎯 What It Does

The AI Order Assistant allows users to create orders using natural language instead of structured JSON, with **intelligent inventory validation** 🆕.

### Example Interactions

| Natural Language Input | What Happens |
|------------------------|--------------|
| "I want 2 laptops for John Doe" | ✅ Creates order with 2 laptops (if inventory available) |
| "Order 3 blue t-shirts size L for Jane Smith" | ✅ Finds blue t-shirts in size L, validates inventory, creates order |
| "Get me the MongoDB Guide book" | ✅ Finds MongoDB book, checks stock, creates order |
| "I need 100 laptops for Bob" | ❌ Returns friendly error: "We only have 10 in stock, would you like to order 10 instead?" |
| "5 laptops and 2 mice for Bob, email bob@example.com" | ✅ Validates inventory for both products, creates order atomically |

---

## 🏗️ Architecture

### LangGraph State Machine

The assistant uses a **5-node state machine** with **transaction support** 🆕 to process requests:

```
User Input (Natural Language)
    ↓
┌─────────────────────────────────────────┐
│  Node 1: Intent Parser (Grove API)     │ ← Extract customer & products
├─────────────────────────────────────────┤
│  Node 2: Customer Resolver (MongoDB)   │ ← Find/create customer
├─────────────────────────────────────────┤
│  Node 3: Product Matcher (MongoDB)     │ ← Fuzzy match products
├─────────────────────────────────────────┤
│  Node 4: Order Creator (MongoDB)       │ ← 🆕 Create order with inventory validation
│         with Transaction Support        │    (uses OrderTransactionService)
│                                         │    - Validates inventory
│                                         │    - Creates order
│                                         │    - Decrements stock
│                                         │    - Rolls back on failure
├─────────────────────────────────────────┤
│  Node 5: Response Generator (Grove)    │ ← Natural language confirmation
│         or Error Handler 🆕             │    or friendly inventory error message
└─────────────────────────────────────────┘
    ↓
Natural Language Response + Order ID
(or inventory error with suggestions)
```

### Technologies Used

- **LangGraph**: State machine orchestration (implemented in Java)
- **Grove API**: OpenAI GPT-5.4 gateway for LLM calls
- **MongoDB Atlas**: Document database with 8 design patterns + ACID transactions 🆕
- **Spring Boot**: REST API framework with @Transactional support
- **Apache Commons Text**: Fuzzy string matching

---

## 🚀 Quick Start

### 1. Configure Grove API

Edit `src/main/resources/application.properties`:

```properties
# Grove API Configuration
grove.api.key=YOUR_GROVE_API_KEY_HERE
grove.api.url=https://grove-gateway-prod.azure-api.net/grove-foundry-prod/openai/v1
grove.api.model=gpt-5.4
grove.api.temperature=0.7
grove.api.max-tokens=1000
```

### 2. Start the Application

```bash
mvn spring-boot:run
```

### 3. Create an Order with Natural Language

```bash
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I want 2 laptops for John Doe"
  }'
```

### 4. Response

```json
{
  "success": true,
  "message": "✅ Great! I've created order #67a1b2c3d4e5f6789 for John Doe with 2 laptops for a total of $2,599.98. Thank you for your order!",
  "orderId": "67a1b2c3d4e5f6789",
  "customerId": "cust123",
  "customerName": "John Doe",
  "total": "2599.98",
  "itemCount": 2
}
```

---

## 📋 API Endpoints

### Create Order from Natural Language

**POST** `/ai/order`

**Request:**
```json
{
  "message": "Order 3 blue t-shirts size L for Jane Smith, email jane@example.com"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "✅ Order created! Order #abc123 for Jane Smith...",
  "orderId": "abc123",
  "customerId": "cust456",
  "customerName": "Jane Smith",
  "total": "89.97",
  "itemCount": 3
}
```

**Response (Product Not Found Error):**
```json
{
  "success": false,
  "message": "I'm sorry, I couldn't find any products matching 'xyz'. Could you try describing the product differently?",
  "error": "No products available in the database"
}
```

**Response (Insufficient Inventory Error) 🆕:**
```json
{
  "success": false,
  "message": "I'm sorry, but we only have 5 'Laptop Pro 15' in stock, but you requested 100. Would you like to order 5 instead?",
  "error": "Insufficient inventory"
}
```

**Response (Multiple Products Insufficient Inventory) 🆕:**
```json
{
  "success": false,
  "message": "I'm sorry, but we don't have enough inventory for your order:\n- Laptop Pro 15: Available 5, Requested 10\n- Wireless Mouse: Available 3, Requested 5\n\nWould you like to adjust your order?",
  "error": "Insufficient inventory for multiple products"
}
```

### Health Check

**GET** `/ai/health`

**Response:**
```json
{
  "status": "healthy",
  "service": "AI Order Assistant",
  "model": "gpt-5.4 via Grove API"
}
```

---

## 🧠 How It Works

### Node 1: Intent Parser

**Input:** Natural language text  
**Output:** Structured `OrderIntent`

**Example:**
```
Input: "I want 2 laptops for John Doe"

Output:
{
  "customerName": "John Doe",
  "products": [
    {
      "productDescription": "laptop",
      "quantity": 2,
      "typeHint": "Electronics"
    }
  ]
}
```

**Technology:** Grove API (GPT-5.4) with structured prompts

---

### Node 2: Customer Resolver

**Input:** `OrderIntent` with customer name  
**Output:** `Customer` entity (found or created)

**Logic:**
1. Search for existing customer by name (case-insensitive)
2. If found: Update email/phone if provided
3. If not found: Create new customer
4. Return customer entity

**MongoDB Pattern:** Uses existing `customers` collection

---

### Node 3: Product Matcher

**Input:** List of product descriptions
**Output:** List of matched products with confidence scores

**Matching Algorithm:**
1. **Exact match**: Product name equals description (score: 1.0)
2. **Contains match**: Name contains description or vice versa (score: 0.8)
3. **Fuzzy match**: Levenshtein distance similarity (score: 0.0-1.0)
4. **Type hint**: Bonus if product type matches (e.g., "Electronics")
5. **Attributes**: Bonus for matching color, size, brand
6. **Description matching** 🆕: Uses product `description` field for better AI matching

**Example:**
```
Description: "blue t-shirt"
Matches: "Cotton T-Shirt" (color: blue, description: "Premium quality cotton t-shirt...") → Score: 0.85
```

**Technology:** Apache Commons Text for fuzzy matching

**Note:** Products now include a `description` field (schema v2) specifically designed for AI-powered fuzzy matching! 🆕

---

### Node 4: Order Creator (with Transaction Support) 🆕

**Input:** Customer + Matched products
**Output:** Created `Order` entity (or inventory error)

**MongoDB Patterns Applied:**
- ✅ **Embedding Pattern**: Items embedded in order (< 50 items)
- ✅ **Subset Pattern**: Customer ID + name copied to order
- ✅ **Computed Pattern**: Total pre-calculated and stored
- ✅ **Outlier Pattern**: Large orders (100+ items) use bucketing
- ✅ **Document Versioning**: Schema version tracked
- ✅ **Transaction Pattern** 🆕: ACID guarantees for order + inventory

**Logic (with Transaction):**
```java
try {
    // START TRANSACTION

    // 1. Validate all products exist
    for (product : products) {
        validateProductExists(product);
    }

    // 2. Check inventory availability
    for (product : products) {
        if (product.inventory < requestedQuantity) {
            throw InsufficientInventoryException;
        }
    }

    // 3. Create OrderItem for each product (with price snapshot)
    // 4. Calculate total (sum of price × quantity)
    // 5. Check item count:
    //    - < 50 items: Embed in order
    //    - 50-99 items: Embed but flag as large
    //    - 100+ items: Use bucketing (separate collection)

    // 6. Save order to MongoDB
    Order order = orderRepository.save(order);

    // 7. Decrement inventory for all products
    for (item : order.items) {
        productRepository.decrementInventory(item.productId, item.quantity);
    }

    // COMMIT - All succeeded!
    return order;

} catch (InsufficientInventoryException e) {
    // ROLLBACK - Generate friendly error message
    return buildInventoryErrorMessage(e);
}
```

**Error Handling:**
- If inventory insufficient: Transaction rolls back, no order created
- Generates natural language error with available quantities
- Suggests alternative quantities to the user

---

### Node 5: Response Generator

**Input:** Created order + full context  
**Output:** Natural language confirmation

**Example:**
```
Input: Order #12345, John Doe, 2 laptops, $2,599.98

Output: "✅ Great! I've created order #12345 for John Doe with 2 laptops for a total of $2,599.98. Thank you for your order!"
```

**Technology:** Grove API (GPT-5.4) with confirmation prompts

---

## 🎨 Example Use Cases

### 1. Simple Order
```bash
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "I want a laptop"}'
```

### 2. Order with Customer Details
```bash
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "Order 2 laptops for Alice Johnson, email alice@example.com"}'
```

### 3. Order with Product Attributes
```bash
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "Get me 3 blue cotton t-shirts in size L for Bob"}'
```

### 4. Multiple Products
```bash
curl -X POST http://localhost:8080/ai/order \
  -H "Content-Type: application/json" \
  -d '{"message": "I need 5 laptops, 10 mice, and 3 keyboards for TechCorp"}'
```

---

## 📊 MongoDB Patterns in Action

The AI Order Assistant demonstrates **all 7 MongoDB design patterns**:

| Pattern | Where Used | Benefit |
|---------|------------|---------|
| **Embedding** | OrderItems in Order | One query gets complete order |
| **Referencing** | Customer ID in Order | Avoid duplicating customer data |
| **Subset** | Customer name in Order | Fast display without join |
| **Computed** | Order total | Pre-calculated for performance |
| **Polymorphic** | Different product types | Flexible schema for products |
| **Versioning** | Schema version field | Track document evolution |
| **Outlier** | Large order bucketing | Handle 100+ items efficiently |

---

## 🔧 Configuration Options

### Grove API Settings

```properties
# Model selection
grove.api.model=gpt-5.4          # or gpt-4, gpt-3.5-turbo

# Temperature (creativity)
grove.api.temperature=0.7        # 0.0 = deterministic, 1.0 = creative

# Max response length
grove.api.max-tokens=1000        # Adjust based on needs

# Timeout
grove.api.timeout-seconds=30     # API call timeout
```

### Product Matching Tuning

Edit `ProductMatcherService.java`:

```java
private static final double MIN_MATCH_SCORE = 0.5;  // Minimum confidence (0.0-1.0)
```

Lower = more lenient matching  
Higher = stricter matching

---

## 🐛 Troubleshooting

### Error: "Failed to call Grove API"

**Cause:** Invalid API key or network issue

**Solution:**
1. Verify `grove.api.key` in `application.properties`
2. Check network connectivity
3. Verify Grove API endpoint is accessible

### Error: "No products available in the database"

**Cause:** Product collection is empty

**Solution:**
```bash
# Add some products first
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Electronics",
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50
  }'
```

### Error: "Customer name is required"

**Cause:** LLM couldn't extract customer name from input

**Solution:** Be more explicit:
- ❌ "I want a laptop"
- ✅ "I want a laptop for John Doe"

---

## 📚 Next Steps

1. **Test the AI Assistant**: Try various natural language inputs
2. **Add Products**: Populate the database with diverse products
3. **Monitor Performance**: Check logs for match scores and timing
4. **Customize Prompts**: Edit system prompts in service classes
5. **Extend Functionality**: Add features like order cancellation, updates

---

## 🎯 Key Takeaways

- ✅ **Natural Language**: Users don't need to know JSON structure
- ✅ **LangGraph**: State machine makes workflow clear and maintainable
- ✅ **Grove API**: Powerful LLM capabilities via simple REST calls
- ✅ **MongoDB Patterns**: All 7 patterns working together seamlessly
- ✅ **Fuzzy Matching**: Handles typos and variations in product names
- ✅ **Production Ready**: Error handling, logging, and fallbacks included

**The AI Order Assistant showcases how modern AI and MongoDB work together to create intelligent, user-friendly applications!** 🚀

