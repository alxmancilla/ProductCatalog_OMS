package com.example.store.controller;

import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Product operations.
 * Demonstrates basic CRUD operations with MongoDB.
 */
@RestController
@RequestMapping("/products")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Create a new product.
     * POST /products
     *
     * Validates that required fields are present:
     * - name (required, not blank)
     * - description (required, not blank)
     * - price (required, positive)
     * - category (required, not blank)
     * - inventory (required, zero or greater)
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
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

