package com.example.store.service;

import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling order creation with inventory management using MongoDB transactions.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: ACID Transactions for Order Creation + Inventory Updates
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service ensures that:
 * 1. Customer exists
 * 2. All products exist and have sufficient inventory
 * 3. Order is created successfully
 * 4. Inventory is decremented for all products
 * 5. If ANY step fails, EVERYTHING is rolled back
 *
 * Transaction Flow:
 * ┌─────────────────────────────────────────┐
 * │ START TRANSACTION                       │
 * ├─────────────────────────────────────────┤
 * │ 1. Validate customer exists             │
 * │ 2. Validate all products exist          │
 * │ 3. Check inventory availability         │
 * │ 4. Create order document                │
 * │ 5. Decrement inventory for each product │
 * ├─────────────────────────────────────────┤
 * │ COMMIT (if all succeed)                 │
 * │ ROLLBACK (if any fail)                  │
 * └─────────────────────────────────────────┘
 *
 * IMPORTANT: Requires MongoDB to be running as a replica set!
 */
@Service
public class OrderTransactionService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;
    
    /**
     * Create an order with inventory validation and updates in a transaction.
     *
     * @param order The order to create
     * @return The saved order
     * @throws CustomerNotFoundException if customer is not found
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientInventoryException if any product has insufficient inventory
     */
    @Transactional
    public Order createOrderWithInventoryUpdate(Order order) {
        // ═══════════════════════════════════════════════════════════════════
        // STEP 0: Validate customer exists
        // ═══════════════════════════════════════════════════════════════════
        String customerId = order.getCustomerId();
        if (customerId != null && !customerId.trim().isEmpty()) {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

            // Update customer name in order if not set or different
            if (order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()) {
                order.setCustomerName(customer.getName());
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Validate all products exist and check inventory
        // ═══════════════════════════════════════════════════════════════════
        Map<String, InventoryInfo> insufficientProducts = new HashMap<>();
        Map<String, Product> productMap = new HashMap<>();
        
        for (OrderItem item : order.getItems()) {
            String productId = item.getProductId();
            
            // Check if product exists
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
            
            productMap.put(productId, product);
            
            // Check if sufficient inventory
            if (product.getInventory() < item.getQuantity()) {
                insufficientProducts.put(
                    productId,
                    new InventoryInfo(
                        product.getName(),
                        item.getQuantity(),
                        product.getInventory()
                    )
                );
            }
        }
        
        // If any products have insufficient inventory, throw exception
        if (!insufficientProducts.isEmpty()) {
            throw new InsufficientInventoryException(insufficientProducts);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Create the order
        // ═══════════════════════════════════════════════════════════════════
        Order savedOrder = orderRepository.save(order);
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Decrement inventory for all products
        // ═══════════════════════════════════════════════════════════════════
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProductId());
            
            // Decrement inventory
            int newInventory = product.getInventory() - item.getQuantity();
            product.setInventory(newInventory);
            
            // Save updated product
            productRepository.save(product);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // Transaction will commit here if all operations succeeded
        // Transaction will rollback if any exception was thrown
        // ═══════════════════════════════════════════════════════════════════
        
        return savedOrder;
    }
}

