package com.example.store.repository;

import com.example.store.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Customer entity.
 * Spring Data MongoDB provides the implementation automatically.
 */
@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    // MongoRepository provides basic CRUD operations:
    // - save()
    // - findById()
    // - findAll()
    // - deleteById()
    // - count()
    // etc.
}

