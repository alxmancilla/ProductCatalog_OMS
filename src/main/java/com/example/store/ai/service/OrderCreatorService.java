package com.example.store.ai.service;

import com.example.store.ai.model.AgentState;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderItemBucket;
import com.example.store.model.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.service.OrderTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for creating orders from resolved products and customer.
 * Implements the Computed Pattern, Outlier Pattern, and Transactions.
 */
@Service
public class OrderCreatorService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatorService.class);
    private static final int LARGE_ORDER_THRESHOLD = 50;
    private static final int BUCKETING_THRESHOLD = 100;
    private static final int BUCKET_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderItemBucketRepository orderItemBucketRepository;
    private final OrderTransactionService orderTransactionService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ApplicationContext applicationContext;

    @Autowired
    public OrderCreatorService(
            OrderRepository orderRepository,
            OrderItemBucketRepository orderItemBucketRepository,
            OrderTransactionService orderTransactionService,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ApplicationContext applicationContext) {
        this.orderRepository = orderRepository;
        this.orderItemBucketRepository = orderItemBucketRepository;
        this.orderTransactionService = orderTransactionService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.applicationContext = applicationContext;
    }
    
    /**
     * Create an order from resolved products and customer.
     * Uses transactions to ensure inventory is validated and updated atomically.
     *
     * @param customer The customer placing the order
     * @param resolvedProducts List of resolved products with quantities
     * @return The created order
     * @throws InsufficientInventoryException if any product has insufficient inventory
     */
    public Order createOrder(Customer customer, List<AgentState.ResolvedProduct> resolvedProducts) {
        if (resolvedProducts == null || resolvedProducts.isEmpty()) {
            throw new IllegalArgumentException("Cannot create order without products");
        }

        // Create order items
        List<OrderItem> items = new ArrayList<>();
        for (AgentState.ResolvedProduct resolved : resolvedProducts) {
            OrderItem item = new OrderItem();
            item.setProductId(resolved.getProduct().getId());
            item.setName(resolved.getProduct().getName());
            item.setPrice(resolved.getProduct().getPrice());
            item.setQuantity(resolved.getQuantity());
            items.add(item);
        }

        // Create order
        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setOrderDate(LocalDateTime.now());
        order.setSchemaVersion(3);

        // COMPUTED PATTERN: Calculate total
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        // OUTLIER PATTERN: Handle large orders
        int itemCount = items.size();

        if (itemCount >= LARGE_ORDER_THRESHOLD) {
            order.setIsLargeOrder(true);
            order.setTotalItemCount(itemCount);

            if (itemCount >= BUCKETING_THRESHOLD) {
                // Very large order - use bucketing (call via proxy to enable @Transactional)
                logger.info("Creating large order with {} items (bucketing)", itemCount);
                OrderCreatorService proxy = applicationContext.getBean(OrderCreatorService.class);
                return proxy.createLargeOrderWithBuckets(order, items);
            } else {
                // Large but not huge - still embed items
                logger.info("Creating large order with {} items (embedded)", itemCount);
                order.setItems(items);
            }
        } else {
            // Normal order - embed items
            logger.info("Creating normal order with {} items", itemCount);
            order.setIsLargeOrder(false);
            order.setItems(items);
        }

        // ═══════════════════════════════════════════════════════════════════
        // TRANSACTION: Create order and update inventory atomically
        // ═══════════════════════════════════════════════════════════════════
        // This will:
        // 1. Validate all products exist
        // 2. Check inventory availability
        // 3. Create the order
        // 4. Decrement inventory for all products
        // 5. Rollback everything if any step fails
        Order savedOrder = orderTransactionService.createOrderWithInventoryUpdate(order);
        logger.info("Created order {} for customer {} with total ${}",
                savedOrder.getId(), customer.getName(), total);

        return savedOrder;
    }
    
    /**
     * Create a large order with bucketing (100+ items).
     * Implements the Outlier Pattern with ACID Transaction support.
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * 🎯 TRANSACTION FLOW for Large Orders (100+ items):
     * ═══════════════════════════════════════════════════════════════════════════
     * 1. Validate customer exists
     * 2. Validate all products exist
     * 3. Check inventory availability for all items
     * 4. Create main order document (metadata only)
     * 5. Create all bucket documents
     * 6. Decrement inventory for all products
     * 7. COMMIT if all succeed, ROLLBACK if any fail
     *
     * NOTE: This must be public so Spring can apply @Transactional via proxy.
     */
    @Transactional
    public Order createLargeOrderWithBuckets(Order order, List<OrderItem> allItems) {
        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Validate customer exists
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
        // STEP 2: Validate all products exist and check inventory
        // ═══════════════════════════════════════════════════════════════════
        java.util.Map<String, InsufficientInventoryException.InventoryInfo> insufficientProducts = new java.util.HashMap<>();
        java.util.Map<String, Product> productMap = new java.util.HashMap<>();

        for (OrderItem item : allItems) {
            String productId = item.getProductId();

            // Check if product exists
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            productMap.put(productId, product);

            // Check if sufficient inventory
            if (product.getInventory() < item.getQuantity()) {
                insufficientProducts.put(
                    productId,
                    new InsufficientInventoryException.InventoryInfo(
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
        // STEP 3: Create the main order document (metadata only)
        // ═══════════════════════════════════════════════════════════════════
        int totalItems = allItems.size();
        int bucketCount = (int) Math.ceil((double) totalItems / BUCKET_SIZE);

        // Update order metadata
        order.setTotalItemCount(totalItems);
        order.setBucketCount(bucketCount);
        order.setItems(null);  // Don't embed items for large orders

        // Save the main order document (without items)
        Order savedOrder = orderRepository.save(order);

        // ═══════════════════════════════════════════════════════════════════
        // STEP 4: Create and save all buckets
        // ═══════════════════════════════════════════════════════════════════
        for (int i = 0; i < bucketCount; i++) {
            int startIndex = i * BUCKET_SIZE;
            int endIndex = Math.min(startIndex + BUCKET_SIZE, totalItems);
            List<OrderItem> bucketItems = allItems.subList(startIndex, endIndex);

            OrderItemBucket bucket = new OrderItemBucket();
            bucket.setId(savedOrder.getId() + "_bucket_" + i);
            bucket.setOrderId(savedOrder.getId());
            bucket.setBucketNumber(i);
            bucket.setItems(bucketItems);

            orderItemBucketRepository.save(bucket);
            logger.debug("Created bucket {} with {} items", i, bucketItems.size());
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 5: Decrement inventory for all products
        // ═══════════════════════════════════════════════════════════════════
        for (OrderItem item : allItems) {
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

        logger.info("Created large order {} with {} buckets and updated inventory",
                    savedOrder.getId(), bucketCount);
        return savedOrder;
    }
}

