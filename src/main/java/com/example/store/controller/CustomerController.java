package com.example.store.controller;

import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Customer operations.
 * Demonstrates basic CRUD operations with MongoDB.
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    /**
     * Create a new customer.
     * POST /customers
     */
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Customer savedCustomer = customerRepository.save(customer);
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    /**
     * Get all customers with pagination (P0 FIX).
     * GET /customers?page=0&size=20
     *
     * 🎯 PAGINATION: Prevents OOM with large customer bases
     * - page: Page number (0-based)
     * - size: Results per page (default: 20, max: 100)
     * - Sorted alphabetically by name
     *
     * Example:
     * - GET /customers          → First 20 customers
     * - GET /customers?size=50  → First 50 customers
     * - GET /customers?page=1&size=20  → Customers 21-40
     */
    @GetMapping
    public ResponseEntity<Page<Customer>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap page size to prevent large scans
        size = Math.min(size, 100);

        PageRequest pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.ASC, "name"));
        Page<Customer> customers = customerRepository.findAll(pageable);

        return ResponseEntity.ok(customers);
    }

    /**
     * Get all orders for a specific customer.
     * GET /customers/{customerId}/orders
     *
     * 🎯 CUSTOMER ORDER HISTORY: Convenience endpoint
     * - Returns all orders for the customer, newest first
     * - Uses index { customerId: 1, orderDate: -1 } for fast queries
     */
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<List<Order>> getCustomerOrders(@PathVariable String customerId) {
        // Validate customer exists
        customerRepository.findById(customerId)
            .orElseThrow(() -> new com.example.store.exception.CustomerNotFoundException(customerId));

        // Get all orders for this customer
        List<Order> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        return ResponseEntity.ok(orders);
    }
}

