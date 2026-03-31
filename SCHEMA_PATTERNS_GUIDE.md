# MongoDB Schema Design Patterns in This Demo

This demo showcases **TEN MongoDB Schema Design Patterns** that solve real-world data modeling challenges, including advanced patterns for handling polymorphic data and transactions.

---

## 🧮 Pattern 1: Computed Pattern

### What Is It?

Pre-calculate values and store them in the document so they're ready when clients request data.

### Where We Use It

**Order Total Calculation** - `OrderController.java`

### The Problem

Without the Computed Pattern:
```javascript
// Every time you query an order, you'd need to calculate:
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: {
      _id: "$_id",
      total: { $sum: { $multiply: ["$items.price", "$items.quantity"] } }
  }}
])
```
- Slow queries (calculation on every read)
- Complex aggregation pipelines
- More CPU usage

### Our Solution

```java
// Calculate ONCE when creating the order
BigDecimal total = order.getItems().stream()
    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
    .reduce(BigDecimal.ZERO, BigDecimal::add);

order.setTotal(total);  // Store it!
```

### The Result

```json
{
  "_id": "order123",
  "items": [
    { "name": "Laptop", "price": 1299.99, "quantity": 1 },
    { "name": "Mouse", "price": 29.99, "quantity": 2 }
  ],
  "total": 1359.97  ← Pre-calculated and stored!
}
```

### Benefits

✅ **Fast reads** - No calculation needed
✅ **Simple queries** - Just read the `total` field
✅ **Consistent** - Calculation logic in one place
✅ **Efficient** - Calculate once, read many times

### When to Use

- Values that are expensive to calculate
- Values that are read frequently
- Values that don't change after creation
- Aggregations (sum, average, count, etc.)

---

## 🎭 Pattern 2: Polymorphic Pattern

### What Is It?

Store documents with different fields in the same collection based on their type.

### Where We Use It

**Product Types** - `Product.java`

### The Problem

Traditional approach (separate tables/collections):
```
electronics_products
clothing_products
book_products
```
- Need to query multiple collections
- Complex union queries
- Hard to add new product types

### Our Solution

**One collection, flexible schema:**

```java
public class Product {
    private String type;  // "Electronics", "Clothing", "Book"

    // Common fields (all products)
    private String name;
    private BigDecimal price;

    // Electronics-specific
    private String warranty;
    private String brand;

    // Clothing-specific
    private String size;
    private String color;

    // Book-specific
    private String author;
    private String isbn;
}
```

### Example Documents

**Electronics:**
```json
{
  "_id": "prod1",
  "type": "Electronics",
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "warranty": "2 years",
  "brand": "TechCorp"
}
```

**Clothing:**
```json
{
  "_id": "prod2",
  "type": "Clothing",
  "name": "Cotton T-Shirt",
  "price": 29.99,
  "size": "L",
  "color": "Blue",
  "material": "100% Cotton"
}
```

**Book:**
```json
{
  "_id": "prod3",
  "type": "Book",
  "name": "MongoDB Guide",
  "price": 49.99,
  "author": "Jane Smith",
  "isbn": "978-1234567890",
  "pages": 350
}
```

### Benefits

✅ **Single collection** - Query all products at once
✅ **Flexible schema** - Each type has unique fields
✅ **Easy to extend** - Add new types without schema changes
✅ **Simpler queries** - No unions or joins needed

### When to Use

- Entities with common base fields but type-specific variations
- When you need to query across all types
- When types share similar behavior
- When new types are added frequently

---

## 🧩 Pattern 2a: Composition Pattern

### What Is It?

Group type-specific fields into embedded objects (nested documents) to create clear, organized, and type-safe structures for polymorphic data.

### Where We Use It

**Product Type-Specific Details** - `Product.java` with `ElectronicsDetails`, `ClothingDetails`, `BookDetails`

### The Problem

**Before Composition (Flat/Optional Fields):**
```java
public class Product {
    // All fields mixed together - confusing!
    private String warranty;      // Only for Electronics
    private String brand;         // Only for Electronics
    private String screenSize;    // Only for Electronics
    private String size;          // Only for Clothing
    private String color;         // Only for Clothing
    private String material;      // Only for Clothing
    private String author;        // Only for Books
    private String isbn;          // Only for Books
    private Integer pages;        // Only for Books
}
```

**Problems:**
- ❌ Cluttered class with 50+ optional fields
- ❌ Unclear which fields belong to which product type
- ❌ Many null fields per document
- ❌ Hard to validate (all fields optional)
- ❌ Difficult to maintain
- ❌ No clear boundaries

### Our Solution

**Group related fields into embedded objects:**

```java
// Separate classes for type-specific fields
public class ElectronicsDetails {
    private String warranty;
    private String brand;
    private String screenSize;
    private String resolution;
    private String capacity;
}

public class ClothingDetails {
    private String size;
    private String color;
    private String material;
}

public class BookDetails {
    private String author;
    private String isbn;
    private Integer pages;
    private String publisher;
    private String language;
}

// Clean Product class
public class Product {
    private String type;  // "Electronics", "Clothing", "Book"

    // Common fields
    private String name;
    private BigDecimal price;

    // Type-specific embedded objects (only one is non-null)
    private ElectronicsDetails electronicsDetails;
    private ClothingDetails clothingDetails;
    private BookDetails bookDetails;
}
```

### Example Documents

**Electronics Product:**
```json
{
  "_id": "prod1",
  "type": "Electronics",
  "name": "Samsung Galaxy S21",
  "price": 799.99,
  "electronicsDetails": {
    "warranty": "2 years",
    "brand": "Samsung",
    "screenSize": "6.2 inches",
    "resolution": "1080x2400",
    "capacity": "128GB"
  },
  "clothingDetails": null,
  "bookDetails": null
}
```

**Clothing Product:**
```json
{
  "_id": "prod2",
  "type": "Clothing",
  "name": "Nike Running Shoes",
  "price": 129.99,
  "electronicsDetails": null,
  "clothingDetails": {
    "size": "10",
    "color": "Black",
    "material": "Synthetic mesh"
  },
  "bookDetails": null
}
```

**Book Product:**
```json
{
  "_id": "prod3",
  "type": "Book",
  "name": "MongoDB: The Definitive Guide",
  "price": 49.99,
  "electronicsDetails": null,
  "clothingDetails": null,
  "bookDetails": {
    "author": "Shannon Bradshaw",
    "isbn": "978-1491954461",
    "pages": 514,
    "publisher": "O'Reilly Media",
    "language": "English"
  }
}
```

### Queries

```javascript
// Find all Samsung electronics
db.products.find({
  "type": "Electronics",
  "electronicsDetails.brand": "Samsung"
})

// Find large clothing items
db.products.find({
  "type": "Clothing",
  "clothingDetails.size": "L"
})

// Find books by author
db.products.find({
  "type": "Book",
  "bookDetails.author": /Shannon/
})
```

### Indexes

```javascript
// Type-specific indexes
db.products.createIndex({ "electronicsDetails.brand": 1 })
db.products.createIndex({ "clothingDetails.size": 1 })
db.products.createIndex({ "bookDetails.author": 1 })
```

### Integration with Strategy Pattern

**Composition Pattern + Strategy Pattern = Clean Validation:**

```java
// Strategy interface
public interface ProductValidator {
    void validate(Product product);
    String getSupportedType();
}

// Electronics validator
@Component
public class ElectronicsValidator implements ProductValidator {
    public void validate(Product product) {
        ElectronicsDetails details = product.getElectronicsDetails();

        if (details == null) {
            throw new ValidationException("Electronics must have electronicsDetails");
        }
        if (details.getWarranty() == null) {
            throw new ValidationException("Warranty is required");
        }
        if (details.getBrand() == null) {
            throw new ValidationException("Brand is required");
        }
    }

    public String getSupportedType() {
        return "Electronics";
    }
}
```

### MongoDB JSON Schema Validation (oneOf)

```java
// Polymorphic validation using oneOf
Document validator = new Document("$jsonSchema", new Document()
    .append("oneOf", Arrays.asList(
        // Electronics: type=Electronics AND electronicsDetails required
        new Document()
            .append("properties", new Document()
                .append("type", new Document().append("enum", Arrays.asList("Electronics"))))
            .append("required", Arrays.asList("electronicsDetails")),

        // Clothing: type=Clothing AND clothingDetails required
        new Document()
            .append("properties", new Document()
                .append("type", new Document().append("enum", Arrays.asList("Clothing"))))
            .append("required", Arrays.asList("clothingDetails")),

        // Book: type=Book AND bookDetails required
        new Document()
            .append("properties", new Document()
                .append("type", new Document().append("enum", Arrays.asList("Book"))))
            .append("required", Arrays.asList("bookDetails"))
    ))
);
```

**This ensures:** IF `type = "Electronics"` THEN `electronicsDetails` MUST exist!

### Benefits

✅ **Clear Structure** - Each type's fields are grouped together
✅ **Type Safety** - Java classes provide compile-time validation
✅ **Self-Documenting** - Code is easy to understand
✅ **Easy Validation** - Strategy Pattern validates type-specific objects
✅ **IDE Support** - Autocomplete works perfectly
✅ **Maintainable** - Adding fields to a type is straightforward
✅ **Database Validation** - MongoDB JSON Schema enforces structure
✅ **Better Organization** - Clear ownership of fields

### When to Use

✅ **Well-defined product types** (3-20 types)
✅ **Known schema at design time**
✅ **Type-specific business logic needed**
✅ **Strong typing requirements** (Java, TypeScript, etc.)
✅ **Clear validation rules per type**
✅ **Developer experience is priority**
✅ **Educational/demo projects** (shows best practices)

### When NOT to Use

❌ **Hundreds of product types** (index explosion risk)
❌ **User-defined custom attributes** (too rigid)
❌ **Unpredictable/rare attributes** (sparse data inefficiency)
❌ **Frequent schema changes** (requires code updates)
❌ **Marketplace with dynamic types** (like Amazon, eBay)

---

## 🔑 Pattern 2b: Attribute Pattern

### What Is It?

Store variable attributes as an array of key-value pairs to handle unpredictable, sparse, or user-defined fields efficiently.

### Where We Could Use It

**Custom Product Attributes** - For marketplace scenarios or user-defined fields

### The Problem

**Scenario:** E-commerce marketplace with thousands of product types:
- Electronics might have: warranty, brand, screenSize, resolution, capacity, weight, color, voltage, wattage...
- Clothing might have: size, color, material, washCare, neckline, sleeveLength, fit, season...
- Books might have: author, isbn, pages, publisher, language, edition, format, dimensions...
- **AND** sellers can add custom attributes!

**With Composition Pattern:**
```java
// This becomes unmanageable with 1000s of fields!
public class ElectronicsDetails {
    private String warranty;
    private String brand;
    // ... 100 more fields
}

// Index explosion!
db.products.createIndex({ "electronicsDetails.warranty": 1 })
db.products.createIndex({ "electronicsDetails.brand": 1 })
// ... 1000 more indexes
```

### The Solution

**Convert fields to key-value pairs:**

```java
public class AttributeKeyValue {
    private String k;  // key
    private String v;  // value
    private String u;  // unit (optional)
}

public class Product {
    private String type;
    private String name;
    private BigDecimal price;

    // All type-specific attributes as key-value pairs
    private List<AttributeKeyValue> attributes;
}
```

### Example Documents

**Electronics Product:**
```json
{
  "_id": "prod1",
  "type": "Electronics",
  "name": "Samsung Galaxy S21",
  "price": 799.99,
  "attributes": [
    { "k": "warranty", "v": "2 years" },
    { "k": "brand", "v": "Samsung" },
    { "k": "screenSize", "v": "6.2", "u": "inches" },
    { "k": "resolution", "v": "1080x2400" },
    { "k": "capacity", "v": "128", "u": "GB" },
    { "k": "5G", "v": "true" },
    { "k": "weight", "v": "169", "u": "grams" }
  ]
}
```

**Clothing Product:**
```json
{
  "_id": "prod2",
  "type": "Clothing",
  "name": "Nike Running Shoes",
  "price": 129.99,
  "attributes": [
    { "k": "size", "v": "10" },
    { "k": "color", "v": "Black" },
    { "k": "material", "v": "Synthetic mesh" },
    { "k": "waterproof", "v": "true" },
    { "k": "weight", "v": "280", "u": "grams" }
  ]
}
```

**Book Product with Custom Attributes:**
```json
{
  "_id": "prod3",
  "type": "Book",
  "name": "MongoDB: The Definitive Guide",
  "price": 49.99,
  "attributes": [
    { "k": "author", "v": "Shannon Bradshaw" },
    { "k": "isbn", "v": "978-1491954461" },
    { "k": "pages", "v": "514" },
    { "k": "publisher", "v": "O'Reilly Media" },
    { "k": "language", "v": "English" },
    { "k": "signed", "v": "true" },
    { "k": "condition", "v": "Like New" }
  ]
}
```

### Queries

```javascript
// Find Samsung products (works for ANY attribute!)
db.products.find({
  "attributes": {
    "$elemMatch": { "k": "brand", "v": "Samsung" }
  }
})

// Find products with warranty of 2 years
db.products.find({
  "attributes": {
    "$elemMatch": { "k": "warranty", "v": "2 years" }
  }
})

// Find products with screenSize > 6 inches (requires aggregation)
db.products.aggregate([
  { "$unwind": "$attributes" },
  { "$match": {
      "attributes.k": "screenSize",
      "attributes.v": { "$gte": "6" }
  }}
])

// Multiple attribute search
db.products.find({
  "$and": [
    { "attributes": { "$elemMatch": { "k": "brand", "v": "Samsung" } } },
    { "attributes": { "$elemMatch": { "k": "5G", "v": "true" } } }
  ]
})
```

### Indexes

**One compound index handles ALL attributes!**

```javascript
// Single index for all key-value pairs
db.products.createIndex({ "attributes.k": 1, "attributes.v": 1 })

// This ONE index supports queries on:
// - warranty, brand, screenSize (Electronics)
// - size, color, material (Clothing)
// - author, isbn, publisher (Books)
// - ANY custom attribute!
```

### Implementation with Helper Methods

```java
public class Product {
    private List<AttributeKeyValue> attributes = new ArrayList<>();

    // Helper method to get attribute value
    public String getAttribute(String key) {
        return attributes.stream()
            .filter(attr -> attr.getK().equals(key))
            .map(AttributeKeyValue::getV)
            .findFirst()
            .orElse(null);
    }

    // Helper method to set attribute
    public void setAttribute(String key, String value, String unit) {
        // Remove existing
        attributes.removeIf(attr -> attr.getK().equals(key));
        // Add new
        attributes.add(new AttributeKeyValue(key, value, unit));
    }

    // Helper to check if attribute exists
    public boolean hasAttribute(String key) {
        return attributes.stream()
            .anyMatch(attr -> attr.getK().equals(key));
    }
}

// Usage
product.setAttribute("warranty", "2 years", null);
String warranty = product.getAttribute("warranty");  // "2 years"
```

### Benefits

✅ **Ultimate Flexibility** - Add new attributes without code changes
✅ **Efficient Indexing** - One index for ALL attributes
✅ **No Index Explosion** - Scales to thousands of attributes
✅ **Sparse Data** - Only store attributes that exist
✅ **User-Defined Fields** - Perfect for marketplaces
✅ **Qualifiers** - Support units, languages, metadata
✅ **Easy Internationalization** - Add `lang` field to attributes
✅ **Dynamic Schema** - No migrations needed

### When to Use

✅ **Hundreds of product types** (Amazon, eBay scale)
✅ **User-defined custom attributes**
✅ **Unpredictable/rare attributes** (many fields used by <1% of products)
✅ **Frequent attribute additions** (new features weekly)
✅ **Marketplace scenarios** (sellers define their own fields)
✅ **Faceted search** (filter by any of 100+ attributes)
✅ **Internationalization** (same attribute in multiple languages)

### When NOT to Use

❌ **Well-defined, stable schema** (use Composition instead)
❌ **Strong typing requirements** (everything becomes strings)
❌ **Complex validation rules** (harder to enforce)
❌ **Simple catalogs** (3-10 product types - use Composition)
❌ **Heavy sorting/aggregation** (array traversal is slower)

### Challenges

⚠️ **Type Safety Loss** - Everything is a string (or needs encoding)
⚠️ **Query Complexity** - `$elemMatch` is verbose
⚠️ **Validation** - Custom logic needed for each attribute
⚠️ **Application Code** - Need helper methods for access
⚠️ **Debugging** - Harder to visualize data
⚠️ **Performance** - Array traversal slower than direct field access

### Hybrid Approach (Best of Both Worlds!)

**Combine Composition + Attribute for optimal results:**

```java
public class Product {
    // Common fields
    private String type;
    private String name;
    private BigDecimal price;

    // Core type-specific fields (Composition Pattern)
    private ElectronicsDetails electronicsDetails;  // warranty, brand

    // Extended/custom attributes (Attribute Pattern)
    private List<AttributeKeyValue> customAttributes;  // user-defined

    // Search facets (Attribute Pattern for filtering)
    private List<SearchFacet> facets;  // optimized for search UI
}
```

**Example:**
```json
{
  "_id": "prod1",
  "type": "Electronics",
  "name": "Samsung Galaxy S21",
  "price": 799.99,

  "electronicsDetails": {
    "warranty": "2 years",
    "brand": "Samsung"
  },

  "customAttributes": [
    { "k": "certified_refurbished", "v": "true" },
    { "k": "seller_note", "v": "Excellent condition" }
  ],

  "facets": [
    { "name": "Brand", "value": "Samsung" },
    { "name": "5G", "value": "Yes" },
    { "name": "Screen Size", "value": "6-7 inches" }
  ]
}
```

**Benefits of Hybrid:**
✅ Core fields: Type-safe (Composition)
✅ Custom fields: Flexible (Attribute)
✅ Search: Optimized (Attribute facets)
✅ Future-proof: Can extend without breaking changes

---

## 📋 Pattern 3: Document Versioning

### What Is It?

Track schema changes over time to handle evolving requirements while maintaining backward compatibility.

### Where We Use It

**Order Schema Evolution** - `Order.java` (currently v3)
**Product Schema Evolution** - `Product.java` (currently v2) 🆕

### The Problem

Your schema needs to change, but you have existing data:
- Old orders use `customer: "John Doe"` (string)
- New orders need `customerId` + `customerName` (Subset Pattern)
- Products need new fields (`inventory`, `sku`, `description`) 🆕
- Can't migrate millions of documents instantly
- Application must handle both formats

### Our Solution

**Track the version:**

```java
// Order.java
public class Order {
    private Integer schemaVersion = 3;  // Track which version (v1 → v2 → v3)
    private String customerId;          // v2 field
    private String customerName;        // v2 field
    private Boolean isLargeOrder;       // v3 field (Outlier Pattern)
}

// Product.java 🆕
public class Product {
    private Integer schemaVersion = 2;  // Track which version (v1 → v2)
    private String description;         // v2 field (for AI matching)
    private Integer inventory;          // v2 field (for transactions)
    private String sku;                 // v2 field (unique identifier)
}
```

### Example Documents

**Order Version 1 (Old format):**
```json
{
  "_id": "order123",
  "customer": "John Doe",
  "orderDate": "2024-01-15",
  "items": [...],
  "total": 1299.99
}
```

**Order Version 3 (Current format):**
```json
{
  "_id": "order456",
  "schemaVersion": 3,
  "customerId": "cust789",
  "customerName": "Jane Smith",
  "orderDate": "2024-02-20",
  "items": [...],
  "isLargeOrder": false,
  "totalItemCount": 2,
  "bucketCount": 0,
  "total": 899.99
}
```

**Product Version 2 (Current format):** 🆕
```json
{
  "_id": "prod123",
  "schemaVersion": 2,
  "name": "Laptop Pro 15",
  "description": "High-performance laptop for professionals",
  "price": 1299.99,
  "category": "Electronics",
  "inventory": 50,
  "sku": "LAPTOP-PRO15-SG"
}
```

### Migration Strategy

**Gradual Migration:**
```java
// When reading orders
if (order.getSchemaVersion() == null || order.getSchemaVersion() == 1) {
    // Handle old format (v1)
    String customerName = order.getCustomer();  // Old field
    // Optionally migrate to v2 format
} else {
    // Handle new format (v2)
    String customerId = order.getCustomerId();
    String customerName = order.getCustomerName();
}
```

**All new orders** are created with `schemaVersion = 2`
**Old orders** can be migrated on-demand or in batches
**Application** handles both versions gracefully

### Benefits

✅ **Safe evolution** - Change schema without breaking existing data
✅ **Gradual migration** - No need to update all documents at once
✅ **Backward compatible** - Application handles old and new formats
✅ **Audit trail** - Know which version each document uses

### When to Use

- Schema needs to evolve over time
- Can't migrate all documents immediately
- Need to support multiple schema versions
- Want to track schema changes for debugging

### Current Schema Versions 🆕

| Collection | Current Version | Evolution |
|------------|----------------|-----------|
| **orders** | v3 | v1 (basic) → v2 (Subset Pattern) → v3 (Outlier Pattern) |
| **products** | v2 | v1 (basic) → v2 (inventory + SKU + description) |
| **customers** | No versioning | Schema stable since creation |
| **order_item_buckets** | No versioning | Created for Outlier Pattern |

**See [PRODUCT_SCHEMA_VERSIONING.md](PRODUCT_SCHEMA_VERSIONING.md) for detailed product schema evolution!**

---

## 📊 Pattern Comparison

| Pattern | Purpose | When to Use | Benefit |
|---------|---------|-------------|---------|
| **Computed** | Pre-calculate values | Expensive calculations, frequent reads | Fast queries |
| **Polymorphic** | Variable fields per type | Multiple entity types in one collection | Schema flexibility |
| **Document Versioning** | Track schema changes | Schema evolution, gradual migration | Safe updates |

---

## 🔍 Polymorphic Product Implementation Comparison

This table compares three approaches to handling polymorphic products with type-specific fields:

| Aspect | **Flat/Optional Fields** | **Composition Pattern** (Current ✅) | **Attribute Pattern** |
|--------|--------------------------|-------------------------------------|----------------------|
| **Structure** | All fields in Product class | Embedded detail objects | Array of key-value pairs |
| **Example** | `warranty`, `brand`, `size` (all optional) | `electronicsDetails: {warranty, brand}` | `attributes: [{k:"warranty", v:"2 years"}]` |
| **Type Safety** | ⚠️ Moderate (nullable fields) | ✅ Excellent (typed classes) | ❌ Poor (everything is string) |
| **Code Clarity** | ❌ Cluttered (all types mixed) | ✅ Excellent (clear separation) | ⚠️ Moderate (generic structure) |
| **Schema Complexity** | ✅ Simple (flat structure) | ⚠️ Moderate (nested objects) | ⚠️ Moderate (array structure) |
| **Null Fields** | ❌ Many nulls per document | ✅ Only one null object | ✅ No nulls (sparse array) |
| **Flexibility** | ❌ Poor (code change needed) | ⚠️ Moderate (new class needed) | ✅ Excellent (no code change) |
| **Query Complexity** | ✅ Simple (`{warranty: "2 years"}`) | ⚠️ Nested (`{electronicsDetails.warranty: "2 years"}`) | ❌ Complex (`{attributes: {$elemMatch: {k:"warranty", v:"2 years"}}}`) |
| **Index Efficiency** | ❌ Index explosion risk | ⚠️ Can explode with many types | ✅ Single compound index |
| **Validation** | ⚠️ Hard (all fields optional) | ✅ Excellent (Strategy + JSON Schema) | ⚠️ Complex (custom logic) |
| **IDE Support** | ✅ Good autocomplete | ✅ Excellent autocomplete | ❌ No autocomplete |
| **Maintenance** | ❌ Confusing (unclear ownership) | ✅ Clean (clear boundaries) | ⚠️ Moderate (helper methods) |
| **Performance** | ✅ Fast (direct field access) | ✅ Fast (direct nested access) | ⚠️ Slower (array traversal) |
| **Best For** | Small prototypes | **Well-defined types** ✅ | Dynamic/unpredictable catalogs |
| **Your Project** | ❌ Don't use | **✅ Perfect fit!** | Consider for custom attributes |

### **Evolution Path (What We Did):**

```
v1: Flat/Optional Fields → v2: Composition Pattern → Future: Hybrid (Composition + Attribute)
      (Messy, unclear)        (Clean, type-safe)         (Best of both worlds)
```

### **Recommended Hybrid Approach:**

```java
public class Product {
    // Common fields
    private String type;
    private String name;
    private BigDecimal price;

    // Core type-specific fields (Composition Pattern) ✅
    private ElectronicsDetails electronicsDetails;
    private ClothingDetails clothingDetails;
    private BookDetails bookDetails;

    // Extended/custom attributes (Attribute Pattern) 🆕
    private List<AttributeKeyValue> customAttributes;

    // Search facets (Attribute Pattern for filtering) 🆕
    private List<SearchFacet> facets;
}
```

**Why Hybrid?**
- ✅ Core attributes: Strong typing (Composition)
- ✅ Custom/rare attributes: Flexibility (Attribute)
- ✅ Search optimization: Efficient indexing (Attribute facets)
- ✅ Future-proof: Can extend without breaking changes

---

## 🎯 How They Work Together in This Demo

### Order Document (All Three Patterns!)

```json
{
  "_id": "order789",
  "schemaVersion": 2,              ← DOCUMENT VERSIONING
  "customerId": "cust123",
  "customerName": "Alice Johnson",
  "orderDate": "2024-03-10",
  "items": [
    {
      "productId": "prod456",
      "name": "Laptop Pro 15",       ← From POLYMORPHIC product
      "price": 1299.99,
      "quantity": 1
    }
  ],
  "total": 1299.99                   ← COMPUTED PATTERN
}
```

### Product Document (Polymorphic Pattern)

```json
{
  "_id": "prod456",
  "type": "Electronics",             ← POLYMORPHIC PATTERN
  "name": "Laptop Pro 15",
  "price": 1299.99,
  "warranty": "2 years",             ← Electronics-specific field
  "brand": "TechCorp"                ← Electronics-specific field
}
```

---

## 🚀 Try It Yourself

### Create a Polymorphic Product (Electronics)

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Electronics",
    "name": "Laptop Pro 15",
    "price": 1299.99,
    "category": "Electronics",
    "inventory": 50,
    "warranty": "2 years",
    "brand": "TechCorp"
  }'
```

### Create a Polymorphic Product (Clothing)

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Clothing",
    "name": "Cotton T-Shirt",
    "price": 29.99,
    "category": "Clothing",
    "inventory": 200,
    "size": "L",
    "color": "Blue",
    "material": "100% Cotton"
  }'
```

### Create an Order (See Computed Pattern in Action)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "<customer-id>",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "<product-id>",
        "name": "Laptop Pro 15",
        "price": 1299.99,
        "quantity": 1
      }
    ]
  }'
```

**Notice:** The `total` field is automatically calculated and the `schemaVersion` is set to 2!

---

## 📚 Learn More

- **[MongoDB Schema Design Patterns](https://www.mongodb.com/blog/post/building-with-patterns-a-summary)** - Official MongoDB blog series
- **[Order.java](src/main/java/com/example/store/model/Order.java)** - See Computed + Versioning patterns
- **[Product.java](src/main/java/com/example/store/model/Product.java)** - See Polymorphic pattern
- **[OrderController.java](src/main/java/com/example/store/controller/OrderController.java)** - See Computed pattern implementation

---

## 🎯 Pattern 4: Outlier Pattern

### What Is It?

Handle cases where most documents are normal-sized, but a few are exceptionally large (outliers).

### Where We Use It

**Large Orders with 100+ Items** - `Order.java` + `OrderItemBucket.java`

### The Problem

- Most orders have 1-20 items (99% of cases)
- Some orders have 100+ items (1% of cases - outliers)
- MongoDB has a 16MB document size limit
- Large embedded arrays hurt performance

### Our Solution

**Adaptive Schema Design:**

```java
// Normal order (< 50 items): Embed items
if (itemCount < 50) {
    order.setItems(items);
    order.setIsLargeOrder(false);
}

// Large order (100+ items): Use bucketing
if (itemCount >= 100) {
    order.setIsLargeOrder(true);
    order.setTotalItemCount(itemCount);
    order.setBucketCount(bucketCount);
    order.setItems(null);  // Items stored in separate buckets

    // Create bucket documents
    for (int i = 0; i < bucketCount; i++) {
        OrderItemBucket bucket = new OrderItemBucket();
        bucket.setOrderId(order.getId());
        bucket.setBucketNumber(i);
        bucket.setItems(bucketItems);  // 50 items per bucket
        orderItemBucketRepository.save(bucket);
    }
}
```

### The Result

**Normal Order (20 items):**
```json
{
  "_id": "order123",
  "isLargeOrder": false,
  "items": [
    {"productId": "prod1", "name": "Laptop", "price": 1299.99, "quantity": 1},
    {"productId": "prod2", "name": "Mouse", "price": 29.99, "quantity": 2}
  ],
  "total": 1359.97
}
```

**Large Order (150 items):**

Main Order Document:
```json
{
  "_id": "order456",
  "isLargeOrder": true,
  "totalItemCount": 150,
  "bucketCount": 3,
  "items": null,  // ← Set to null for large orders
  "total": 15000.00
}
```

Bucket Documents (in `order_item_buckets` collection):
```json
{
  "_id": "order456_bucket_0",
  "orderId": "order456",
  "bucketNumber": 0,
  "items": [ /* 50 items */ ]
}
```

### Schema Validation 🆕

**Important:** MongoDB schema validation allows `items` to be `null` for large orders:

```java
// MongoSchemaValidation.java
.append("items", new Document()
    .append("bsonType", Arrays.asList("array", "null"))  // ← Allows null!
    .append("description", "Order items array (null for large orders using Outlier Pattern)")
)
```

**Why?** The `items` field is NOT in the required fields list to support the Outlier Pattern. This allows:
- ✅ Regular orders: `items` is an array
- ✅ Large orders: `items` is `null` (items stored in buckets)

### Benefits

✅ **Optimizes for common case** (99% of orders are small)
✅ **Handles outliers gracefully** (1% of orders are large)
✅ **Stays below 16MB limit** (buckets are manageable size)
✅ **Maintains performance** (small orders are fast)
✅ **Scalable** (can handle 1000s of items)
✅ **Schema validation supports both patterns** (array or null) 🆕

### When to Use

- Most documents are small, but a few are large
- You want to optimize for the common case
- You need to handle unbounded growth
- You want to stay well below MongoDB's limits

**See [OUTLIER_PATTERN_GUIDE.md](OUTLIER_PATTERN_GUIDE.md) for comprehensive details!**

---

## 🔄 Pattern 8: Transaction Pattern 🆕

### What Is It?

Use MongoDB ACID transactions to ensure multiple operations across collections succeed or fail together atomically.

### Where We Use It

**Order Creation with Inventory Management** - `OrderTransactionService.java`

### The Problem

Without transactions:
```java
// ❌ DANGEROUS: Race conditions and inconsistent data!
Order order = orderRepository.save(order);  // Step 1: Create order

// Step 2: Decrement inventory (might fail!)
for (OrderItem item : order.getItems()) {
    Product product = productRepository.findById(item.getProductId());
    product.setInventory(product.getInventory() - item.getQuantity());
    productRepository.save(product);  // What if this fails?
}
// Result: Order created but inventory not updated! 😱
```

**Problems:**
- Order created even if inventory insufficient
- Partial updates if one product fails
- Race conditions with concurrent orders
- Data inconsistency

### Our Solution

```java
@Transactional  // ✅ All-or-nothing guarantee!
public Order createOrderWithInventoryUpdate(Order order) {
    // START TRANSACTION

    // 1. Validate all products exist
    for (OrderItem item : order.getItems()) {
        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(...));

        // 2. Check inventory availability
        if (product.getInventory() < item.getQuantity()) {
            throw new InsufficientInventoryException(...);
        }
    }

    // 3. Create order
    Order savedOrder = orderRepository.save(order);

    // 4. Decrement inventory for all products
    for (OrderItem item : order.getItems()) {
        productRepository.decrementInventory(
            item.getProductId(),
            item.getQuantity()
        );
    }

    // COMMIT (if all succeed) or ROLLBACK (if any fail)
    return savedOrder;
}
```

### The Result

**Successful Order:**
```json
// Order created
{
  "_id": "order123",
  "customerId": "cust456",
  "items": [
    { "productId": "prod789", "quantity": 2 }
  ],
  "total": 2599.98
}

// Inventory decremented atomically
{
  "_id": "prod789",
  "name": "Laptop Pro 15",
  "inventory": 8  // Was 10, now 8 (10 - 2)
}
```

**Insufficient Inventory (Transaction Rollback):**
```json
// HTTP 400 Bad Request
{
  "error": "Insufficient inventory for product 'Laptop Pro 15'. Available: 8, Requested: 100"
}

// No order created, inventory unchanged!
```

### Benefits

✅ **Atomicity** - All operations succeed or all fail together
✅ **Consistency** - Data always in valid state
✅ **Isolation** - Concurrent transactions don't interfere
✅ **Durability** - Committed changes are permanent
✅ **No Overselling** - Can't sell more than available
✅ **Automatic Rollback** - Failed operations don't leave partial data

### When to Use

- Operations that span multiple documents/collections
- Inventory management and stock control
- Financial transactions
- Any operation requiring all-or-nothing guarantee
- Preventing race conditions

### Requirements

⚠️ **MongoDB transactions require a replica set!**

```yaml
# docker-compose.yml
services:
  mongodb:
    image: mongo:8
    command: --replSet rs0  # Enable replica set
    healthcheck:
      test: ["CMD", "bash", "/docker-entrypoint-initdb.d/init-replica-set.sh"]
```

Even a single-node replica set works for development!

**See [TRANSACTIONS_GUIDE.md](TRANSACTIONS_GUIDE.md) for comprehensive details!**

---

## 🎓 Key Takeaways

1. **Embedding Pattern** = Store related data together (OrderItems in Orders)
2. **Subset Pattern** = Denormalize frequently accessed data
3. **Reference Pattern** = Link between collections when needed
4. **Computed Pattern** = Calculate once, read many times
5. **Polymorphic Pattern** = One collection, flexible schema
6. **Composition Pattern** = Group type-specific fields into embedded objects for clarity and type safety
7. **Attribute Pattern** = Store variable attributes as key-value pairs for ultimate flexibility
8. **Document Versioning** = Safe schema evolution
9. **Outlier Pattern** = Optimize for common case, handle outliers gracefully
10. **Transaction Pattern** 🆕 = ACID guarantees for multi-document operations

These patterns solve real-world problems and showcase MongoDB's flexibility and power! 🚀


