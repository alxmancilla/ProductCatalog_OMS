package com.example.store.controller;

import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
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
     * Get all customers.
     * GET /customers
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
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

