# 🛡️ Validation Architecture - Defense in Depth

## Overview
This project implements **two layers of validation** to ensure data quality:
1. **Application Layer** - Spring Boot validation
2. **Database Layer** - MongoDB schema validation

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Request                             │
│                    (POST /products)                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   LAYER 1: Spring Boot                           │
│                   Application Validation                         │
├─────────────────────────────────────────────────────────────────┤
│  ProductController.java                                          │
│  ├─ @Valid annotation triggers validation                       │
│  └─ Validates @RequestBody Product                              │
│                                                                  │
│  Product.java                                                    │
│  ├─ @NotBlank(message = "Product name is required")            │
│  ├─ @NotBlank(message = "Product description is required")     │
│  ├─ @NotNull + @Positive (price)                               │
│  ├─ @NotBlank (category)                                       │
│  └─ @NotNull + @PositiveOrZero (inventory)                     │
│                                                                  │
│  GlobalExceptionHandler.java                                     │
│  └─ Returns user-friendly JSON error messages                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ✅ Valid │ ❌ Invalid
                             │    │
                             │    └──────────────┐
                             │                   ▼
                             │         ┌──────────────────────┐
                             │         │  400 Bad Request     │
                             │         │  {                   │
                             │         │    "success": false, │
                             │         │    "errors": {...}   │
                             │         │  }                   │
                             │         └──────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   LAYER 2: MongoDB                               │
│                   Schema Validation                              │
├─────────────────────────────────────────────────────────────────┤
│  MongoSchemaValidation.java                                      │
│  ├─ Runs at application startup                                 │
│  ├─ Creates collections with JSON Schema validation             │
│  └─ Enforces rules at database level                            │
│                                                                  │
│  JSON Schema Rules:                                              │
│  {                                                               │
│    "$jsonSchema": {                                              │
│      "bsonType": "object",                                       │
│      "required": ["name", "description", "price", ...],          │
│      "properties": {                                             │
│        "name": { "bsonType": "string", "minLength": 1 },        │
│        "price": { "bsonType": "decimal", "minimum": 0.01 },     │
│        ...                                                       │
│      }                                                           │
│    }                                                             │
│  }                                                               │
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
│  ├─ All documents validated                                     │
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
| `Product.java` | Model with `@NotBlank`, `@NotNull`, `@Positive` annotations |
| `ProductController.java` | Controller with `@Valid` annotation |
| `GlobalExceptionHandler.java` | Handles validation exceptions, returns JSON errors |
| `MongoSchemaValidation.java` | Sets up MongoDB JSON Schema validation |
| `pom.xml` | Includes `spring-boot-starter-validation` dependency |

---

## 🎬 Demo Script for Presentation

1. **Show Spring Boot validation**
   - Try to create product without description via API
   - Show user-friendly error message

2. **Show MongoDB validation**
   - Open mongosh
   - Try to insert invalid document directly
   - Show MongoDB rejection

3. **Explain defense in depth**
   - Two layers of protection
   - Spring Boot for UX, MongoDB for integrity
   - Best practice for production systems

4. **Show in MongoDB Compass**
   - Navigate to products collection
   - Click "Validation" tab
   - Show JSON Schema rules

---

## ✅ Benefits for Your Presentation

- **Professional Architecture** - Industry best practices
- **Data Quality** - Multiple layers of protection
- **MongoDB Features** - Showcase JSON Schema validation
- **Real-World** - How production systems should work
- **Educational** - Clear separation of concerns

---

**This architecture demonstrates enterprise-grade data quality enforcement! 🚀**

