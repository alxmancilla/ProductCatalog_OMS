# 🛡️ Validation Architecture - Defense in Depth

## Overview

This project demonstrates **three-layer validation** for polymorphic product data:

1. **🎯 Layer 1: Basic Field Validation** - Spring Boot `@Valid` annotations
2. **🏭 Layer 2: Strategy Pattern** - Type-specific business logic validation
3. **📋 Layer 3: MongoDB JSON Schema** - Server-side polymorphic validation

---

## The Problem: Polymorphic Product Validation

Products come in different types with unique required fields:

| Product Type | Type-Specific Fields (Required) |
|-------------|--------------------------------|
| **Electronics** | `electronicsDetails`: warranty, brand |
| **Clothing** | `clothingDetails`: size, color, material |
| **Book** | `bookDetails`: author, isbn, pages |

### Challenges:
- ❌ How to validate type-specific fields without messy if/else chains?
- ❌ How to ensure data integrity at both application and database levels?
- ❌ How to make it easy to add new product types?
- ❌ How to keep validation logic separate from the data model?

---

## The Solution: Three-Layer Validation Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Request                             │
│                    POST /products (JSON)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  🎯 LAYER 1: Basic Field Validation (@Valid)                    │
├─────────────────────────────────────────────────────────────────┤
│  ProductController.java                                          │
│  └─ @Valid @RequestBody Product                                 │
│                                                                  │
│  Product.java                                                    │
│  ├─ @NotBlank name                                              │
│  ├─ @NotBlank description                                       │
│  ├─ @Positive price                                             │
│  ├─ @NotBlank category                                          │
│  ├─ @PositiveOrZero inventory                                   │
│  └─ @NotBlank sku                                               │
│                                                                  │
│  ✅ Validates: Common fields all products must have             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  🏭 LAYER 2: Strategy Pattern (Type-Specific Validation)        │
├─────────────────────────────────────────────────────────────────┤
│  ProductValidationService.java                                   │
│  └─ Routes to correct validator based on product.type          │
│                                                                  │
│  ProductValidator Interface                                      │
│  ├─ ElectronicsValidator.java                                   │
│  │   └─ Validates electronicsDetails.warranty & brand          │
│  ├─ ClothingValidator.java                                      │
│  │   └─ Validates clothingDetails.size, color, material        │
│  └─ BookValidator.java                                          │
│      └─ Validates bookDetails.author, isbn, pages              │
│                                                                  │
│  ✅ Validates: Type-specific business rules                     │
│  ✅ Easy to add new product types (just add new validator)     │
│  ✅ Separation of concerns (validation ≠ data model)           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  📋 LAYER 3: MongoDB JSON Schema (Polymorphic Validation)       │
├─────────────────────────────────────────────────────────────────┤
│  MongoSchemaValidation.java                                      │
│  └─ Sets up polymorphic JSON Schema using oneOf                │
│                                                                  │
│  Common Fields (all products):                                   │
│  ├─ name, description, price, category, inventory, sku          │
│  └─ type: enum ["Electronics", "Clothing", "Book"]             │
│                                                                  │
│  Polymorphic Validation (oneOf):                                 │
│  ├─ IF type = "Electronics"                                     │
│  │   THEN electronicsDetails is REQUIRED                        │
│  │        └─ warranty & brand are REQUIRED                      │
│  ├─ IF type = "Clothing"                                        │
│  │   THEN clothingDetails is REQUIRED                           │
│  │        └─ size, color, material are REQUIRED                 │
│  └─ IF type = "Book"                                            │
│      THEN bookDetails is REQUIRED                               │
│           └─ author, isbn, pages are REQUIRED                   │
│                                                                  │
│  ✅ Server-side polymorphic data integrity                      │
│  ✅ Prevents bad data from other sources (scripts, imports)     │
│  ✅ Last line of defense                                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ✅ Valid │ ❌ Invalid
                             │    │
                             │    └──────────────┐
                             │                   ▼
                             │         ┌──────────────────────┐
                             │         │  MongoServerError    │
                             │         │  Document failed     │
                             │         │  validation          │
                             │         └──────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB Database                              │
│                    (product_catalog_oms)                         │
├─────────────────────────────────────────────────────────────────┤
│  products collection                                             │
│  ├─ All documents validated (polymorphic)                       │
│  ├─ Schema visible in MongoDB Compass                           │
│  └─ Protected from all data sources                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow Examples

### ✅ **Valid Product Request**

```
1. User sends POST /products with valid data
   ↓
2. Spring Boot @Valid validates annotations
   ✅ All fields present and valid
   ↓
3. MongoDB schema validation checks JSON Schema
   ✅ Document matches schema
   ↓
4. Document saved to database
   ↓
5. Return 201 Created with product data
```

---

### ❌ **Invalid Product Request (Missing Description)**

```
1. User sends POST /products without description
   ↓
2. Spring Boot @Valid validates annotations
   ❌ @NotBlank(description) fails
   ↓
3. GlobalExceptionHandler catches MethodArgumentNotValidException
   ↓
4. Return 400 Bad Request with error details:
   {
     "success": false,
     "message": "Validation failed",
     "errors": {
       "description": "Product description is required"
     }
   }
   
MongoDB validation never reached (caught early)
```

---

### ❌ **Direct MongoDB Insert (Bypassing Spring Boot)**

```
1. User connects with mongosh
   ↓
2. Runs: db.products.insertOne({name: "", price: -10})
   ↓
3. Spring Boot validation bypassed (not using API)
   ↓
4. MongoDB schema validation checks JSON Schema
   ❌ name is empty (minLength: 1)
   ❌ price is negative (minimum: 0.01)
   ❌ missing required fields (description, category, inventory)
   ↓
5. MongoDB rejects with MongoServerError:
   "Document failed validation"
   
Database protected even without Spring Boot! 🛡️
```

---

## 📊 Comparison Table

| Aspect | Spring Boot Validation | MongoDB Schema Validation |
|--------|----------------------|--------------------------|
| **Layer** | Application | Database |
| **Technology** | Jakarta Validation (`@Valid`) | JSON Schema |
| **Scope** | API requests only | All data sources |
| **Error Messages** | User-friendly, customizable | Technical, detailed |
| **Performance** | Fast (in-memory) | Slightly slower (database) |
| **Visibility** | Code annotations | Database metadata |
| **Bypass Risk** | Can be bypassed (mongosh, Compass) | Cannot be bypassed |
| **Best For** | User experience | Data integrity |

---

## 🎯 Why Both?

### **Spring Boot Validation Alone**
```
✅ Fast feedback
✅ User-friendly errors
❌ Can be bypassed
❌ Only protects API
```

### **MongoDB Validation Alone**
```
✅ Protects all sources
✅ Cannot be bypassed
❌ Technical errors
❌ Slower feedback
```

### **Both Together (Defense in Depth)**
```
✅ Fast, user-friendly feedback (Spring Boot)
✅ Final enforcement (MongoDB)
✅ Protection from all sources
✅ Best of both worlds
```

---

## 🧪 Testing Both Layers

### **Test Spring Boot Validation**
```bash
# Missing description - caught by Spring Boot
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "price": 99.99, "category": "Test", "inventory": 10}'

# Returns 400 with user-friendly error
```

### **Test MongoDB Validation**
```javascript
// In mongosh - bypasses Spring Boot
use product_catalog_oms

db.products.insertOne({
  name: "",
  price: -10.00
})

// MongoDB rejects with validation error
```

---

## 📚 Files Involved

| File | Purpose |
|------|---------|
| `Product.java` | Model with `@NotBlank`, `@NotNull`, `@Positive` + embedded details |
| `ElectronicsDetails.java` | Embedded electronics-specific fields |
| `ClothingDetails.java` | Embedded clothing-specific fields |
| `BookDetails.java` | Embedded book-specific fields |
| `ProductValidator.java` | Strategy interface for type-specific validation |
| `ElectronicsValidator.java` | Validates electronics products |
| `ClothingValidator.java` | Validates clothing products |
| `BookValidator.java` | Validates book products |
| `ProductValidationService.java` | Orchestrates validators (Context) |
| `ProductController.java` | Controller with `@Valid` + ValidationService |
| `MongoSchemaValidation.java` | Sets up polymorphic JSON Schema (oneOf) |

---

## 🏭 Design Patterns Used

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Strategy Pattern** | ProductValidator + implementations | Type-specific validation logic |
| **Composition Pattern** | Product + Detail classes | Group type-specific fields in embedded objects |
| **Factory Pattern** | ProductValidationService | Auto-discover and route to correct validator |
| **Polymorphic Pattern** | MongoDB oneOf schema | Database-level type-specific validation |

---

## 🎬 Demo Script for Presentation

1. **Show Strategy Pattern in action**
   - Try to create Electronics without warranty → ValidationException
   - Try to create Clothing without size → ValidationException
   - Show how easy it is to add a new product type

2. **Show MongoDB polymorphic validation**
   - Open mongosh
   - Try to insert Electronics without electronicsDetails → rejected
   - Try to insert Clothing with electronicsDetails → rejected

3. **Explain three-layer defense**
   - Layer 1: @Valid for common fields
   - Layer 2: Strategy Pattern for business logic
   - Layer 3: MongoDB oneOf for data integrity

4. **Show in MongoDB Compass**
   - Navigate to products collection
   - Click "Validation" tab
   - Show polymorphic JSON Schema with oneOf

---

## ✅ Benefits for Your Presentation

- **Professional Architecture** - Industry design patterns (Strategy + Composition)
- **Maintainable Code** - Easy to add new product types
- **Data Quality** - Three layers of validation protection
- **MongoDB Features** - Polymorphic JSON Schema with oneOf
- **Real-World** - How enterprise systems handle polymorphism
- **Educational** - Clear separation of concerns

---

**This architecture demonstrates enterprise-grade polymorphic data validation! 🚀**

