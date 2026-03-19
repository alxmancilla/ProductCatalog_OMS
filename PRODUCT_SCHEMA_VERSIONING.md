# Product Schema Versioning

## 📋 Overview

The `products` collection now implements the **Document Versioning Pattern** to track schema evolution over time.

---

## 🔄 Schema Evolution History

### **Version 1 (Original - Basic Product)**

**Fields:**
- `_id` - Product ID
- `name` - Product name
- `price` - Product price
- `category` - Product category

**Example:**
```json
{
  "_id": "prod123",
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "category": "Electronics"
}
```

**Limitations:**
- ❌ No inventory tracking
- ❌ No unique SKU identifier
- ❌ No detailed description for AI matching
- ❌ No schema version tracking

---

### **Version 2 (Current - Inventory & SKU Added)** 🆕

**New Fields Added:**
- `schemaVersion` - Version tracking (value: 2)
- `description` - Detailed product description (REQUIRED)
- `inventory` - Stock quantity (REQUIRED)
- `sku` - Stock Keeping Unit (REQUIRED)

**Example:**
```json
{
  "_id": "prod456",
  "schemaVersion": 2,
  "type": "Electronics",
  "name": "Laptop Pro 15",
  "description": "High-performance laptop for professionals",
  "price": 1299.99,
  "category": "Electronics",
  "inventory": 50,
  "sku": "LAPTOP-001",
  "warranty": "2 years",
  "brand": "TechCorp"
}
```

**Benefits:**
- ✅ Inventory management with transactions
- ✅ Unique product identification via SKU
- ✅ Rich descriptions for AI-powered product matching
- ✅ Schema version tracking for future evolution

---

## 🎯 Why Version 2?

### **Inventory Management**
Version 2 adds the `inventory` field to support:
- Real-time stock tracking
- ACID transactions for order creation
- Prevention of overselling
- Automatic inventory decrements

### **SKU Requirement**
Version 2 adds the `sku` field to provide:
- Unique product identifiers (industry standard)
- Better inventory management
- Integration with external systems
- Barcode/scanning support

### **AI Product Matching**
Version 2 adds the `description` field to enable:
- Natural language product search
- AI-powered fuzzy matching
- Better user experience with the AI Order Assistant

---

## 📊 Current Status

| Collection | Current Version | Version Field | Status |
|------------|----------------|---------------|--------|
| **products** | **Version 2** | `schemaVersion = 2` | ✅ Active |
| **orders** | **Version 3** | `schemaVersion = 3` | ✅ Active |
| **customers** | No versioning | N/A | Stable |
| **order_item_buckets** | No versioning | N/A | Stable |

---

## 🔧 Implementation Details

### **Java Model**
```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    
    // 📋 DOCUMENT VERSIONING PATTERN
    private Integer schemaVersion = 2;  // Current version
    
    // Common fields
    private String type;
    private String name;
    private String description;  // Added in v2
    private BigDecimal price;
    private String category;
    private Integer inventory;   // Added in v2
    private String sku;          // Added in v2
    
    // Polymorphic fields (Electronics, Clothing, Books)
    // ...
}
```

### **MongoDB Document**
All new products are automatically created with `schemaVersion: 2`.

---

## 🚀 Migration Strategy

### **For Existing Products (v1)**

If you have existing products without version tracking:

1. **Identify v1 products:**
   ```javascript
   db.products.find({ schemaVersion: { $exists: false } })
   ```

2. **Migrate to v2:**
   ```javascript
   db.products.updateMany(
     { schemaVersion: { $exists: false } },
     { 
       $set: { 
         schemaVersion: 2,
         description: "Product description",  // Add default or specific description
         inventory: 0,                        // Set initial inventory
         sku: "SKU-XXXXX"                     // Generate unique SKU
       }
     }
   )
   ```

3. **Verify migration:**
   ```javascript
   db.products.find({ schemaVersion: 2 }).count()
   ```

### **For New Products**

All new products created via the API automatically include:
- `schemaVersion: 2`
- All required fields (name, description, price, category, inventory, sku)

---

## 💡 Benefits of Schema Versioning

1. ✅ **Backward Compatibility** - Old and new documents coexist
2. ✅ **Gradual Migration** - Migrate documents on-demand, not all at once
3. ✅ **Safe Evolution** - Track changes without breaking existing data
4. ✅ **Clear History** - Document what changed and when
5. ✅ **Flexible Deployment** - Application handles multiple versions

---

## 📚 Related Documentation

- **[SCHEMA_PATTERNS_GUIDE.md](SCHEMA_PATTERNS_GUIDE.md)** - All 8 MongoDB design patterns
- **[TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md)** - Inventory management with transactions
- **[SKU_REQUIREMENT_SUMMARY.md](SKU_REQUIREMENT_SUMMARY.md)** - SKU field requirements
- **[AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md)** - AI-powered product matching

---

**Schema versioning enables safe, gradual evolution of your MongoDB collections! 🚀**

