package com.example.store.controller;

import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import com.example.store.service.ProductValidationService;
import com.example.store.validation.ProductValidationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Product operations.
 * Demonstrates basic CRUD operations with MongoDB.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: STRATEGY PATTERN for Polymorphic Validation
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This controller uses ProductValidationService to perform type-specific
 * validation based on the product type. The validation strategy is selected
 * at runtime without the controller needing to know the validation rules.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductValidationService productValidationService;

    /**
     * Create a new product.
     * POST /products
     *
     * Validates that required fields are present:
     * - Common fields: name, description, price, category, inventory (via @Valid)
     * - Type-specific fields: validated by ProductValidationService based on product type
     *
     * Example request for Electronics:
     * {
     *   "type": "Electronics",
     *   "name": "Laptop Pro 15",
     *   "description": "High-performance laptop",
     *   "price": 1299.99,
     *   "category": "Electronics",
     *   "inventory": 50,
     *   "sku": "LAPTOP-PRO15",
     *   "electronicsDetails": {
     *     "warranty": "2 years",
     *     "brand": "TechCorp",
     *     "weight": "1.8 kg",
     *     "screenSize": "15.6 inch"
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        // Perform type-specific validation using Strategy Pattern
        productValidationService.validate(product);

        // Save the product to MongoDB
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    /**
     * Exception handler for type-specific validation errors.
     * Returns a 400 Bad Request with detailed error information.
     */
    @ExceptionHandler(ProductValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ProductValidationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Validation failed");
        error.put("message", ex.getMessage());
        if (ex.getProductType() != null) {
            error.put("productType", ex.getProductType());
        }
        if (ex.getFieldName() != null) {
            error.put("field", ex.getFieldName());
        }
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Get all products.
     * GET /products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }
}

