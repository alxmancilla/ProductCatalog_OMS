package com.example.store.repository;

import com.example.store.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Order entity.
 * Spring Data MongoDB provides the implementation automatically.
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    // MongoRepository provides basic CRUD operations:
    // - save()
    // - findById()
    // - findAll()
    // - deleteById()
    // - count()
    // etc.
}

