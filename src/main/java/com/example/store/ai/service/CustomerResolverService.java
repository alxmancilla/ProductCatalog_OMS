package com.example.store.ai.service;

import com.example.store.ai.model.OrderIntent;
import com.example.store.model.Customer;
import com.example.store.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for resolving customers from natural language descriptions.
 * Finds existing customers or creates new ones.
 */
@Service
public class CustomerResolverService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerResolverService.class);
    
    private final CustomerRepository customerRepository;
    
    @Autowired
    public CustomerResolverService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    /**
     * Resolve a customer from the parsed intent.
     * Tries to find an existing customer by name, or creates a new one.
     *
     * @param intent The parsed order intent
     * @return The resolved or created customer
     */
    public Customer resolveCustomer(OrderIntent intent) {
        String customerName = intent.getCustomerName();
        
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        // Try to find existing customer by name (case-insensitive)
        Optional<Customer> existingCustomer = findCustomerByName(customerName);
        
        if (existingCustomer.isPresent()) {
            logger.info("Found existing customer: {}", customerName);
            Customer customer = existingCustomer.get();
            
            // Update email/phone if provided and different
            boolean updated = false;
            if (intent.getCustomerEmail() != null && !intent.getCustomerEmail().equals(customer.getEmail())) {
                customer.setEmail(intent.getCustomerEmail());
                updated = true;
            }
            if (intent.getCustomerPhone() != null && !intent.getCustomerPhone().equals(customer.getPhone())) {
                customer.setPhone(intent.getCustomerPhone());
                updated = true;
            }
            
            if (updated) {
                customer = customerRepository.save(customer);
                logger.info("Updated customer information for: {}", customerName);
            }
            
            return customer;
        } else {
            // Create new customer
            Customer newCustomer = new Customer();
            newCustomer.setName(customerName);
            newCustomer.setEmail(intent.getCustomerEmail());
            newCustomer.setPhone(intent.getCustomerPhone());
            
            newCustomer = customerRepository.save(newCustomer);
            logger.info("Created new customer: {} (ID: {})", customerName, newCustomer.getId());
            
            return newCustomer;
        }
    }
    
    /**
     * Find a customer by name (case-insensitive).
     *
     * @param name Customer name
     * @return Optional containing the customer if found
     */
    private Optional<Customer> findCustomerByName(String name) {
        List<Customer> allCustomers = customerRepository.findAll();
        
        return allCustomers.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}

