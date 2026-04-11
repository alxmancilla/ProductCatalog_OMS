package com.example.store.repository;

import com.example.store.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Product entity.
 * Spring Data MongoDB provides the implementation automatically.
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // MongoRepository provides basic CRUD operations:
    // - save()
    // - findById()
    // - findAll()
    // - deleteById()
    // - count()
    // etc.

    Optional<Product> findBySku(String sku);
}

