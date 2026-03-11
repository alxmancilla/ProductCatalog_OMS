package com.example.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for Product Catalog + OMS Demo.
 * 
 * This demo showcases:
 * - MongoDB document storage with JSON-like documents
 * - Natural mapping between Java objects and MongoDB documents
 * - REST API implementation with Spring Boot
 * - Embedded documents (OrderItems within Orders)
 * - Basic CRUD operations
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

