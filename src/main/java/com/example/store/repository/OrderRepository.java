package com.example.store.repository;

import com.example.store.model.Order;
import com.example.store.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Order entity.
 * Spring Data MongoDB provides the implementation automatically.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 SEARCH & FILTERING: MongoDB-Optimized Query Methods
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * All query methods use indexes for optimal performance:
 * - { customerId: 1, orderDate: -1 }
 * - { status: 1, orderDate: -1 }
 * - { orderDate: -1 }
 * - { total: 1 }
 * - { customerId: 1, status: 1, orderDate: -1 }
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

    /**
     * Find all orders with a specific status.
     * Spring Data MongoDB will automatically implement this based on the method name.
     *
     * Query: db.orders.find({ "status": <status> })
     * Index: { status: 1, orderDate: -1 }
     *
     * @param status The order status to filter by
     * @return List of orders with the given status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find all orders for a specific customer, ordered by date (newest first).
     *
     * Query: db.orders.find({ "customerId": <customerId> }).sort({ "orderDate": -1 })
     * Index: { customerId: 1, orderDate: -1 }
     *
     * @param customerId The customer ID
     * @return List of orders for the customer, newest first
     */
    List<Order> findByCustomerIdOrderByOrderDateDesc(String customerId);

    /**
     * Find all orders with a specific status, ordered by date (newest first).
     *
     * Query: db.orders.find({ "status": <status> }).sort({ "orderDate": -1 })
     * Index: { status: 1, orderDate: -1 }
     *
     * @param status The order status
     * @return List of orders with the status, newest first
     */
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);

    /**
     * Find all orders within a date range, ordered by date (newest first).
     *
     * Query: db.orders.find({ "orderDate": { $gte: <start>, $lte: <end> } }).sort({ "orderDate": -1 })
     * Index: { orderDate: -1 }
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of orders in date range, newest first
     */
    List<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all orders within a total amount range.
     *
     * Query: db.orders.find({ "total": { $gte: <minTotal>, $lte: <maxTotal> } })
     * Index: { total: 1 }
     *
     * @param minTotal Minimum order total
     * @param maxTotal Maximum order total
     * @return List of orders in price range
     */
    List<Order> findByTotalBetween(BigDecimal minTotal, BigDecimal maxTotal);

    /**
     * Find all orders for a customer with a specific status.
     *
     * Query: db.orders.find({ "customerId": <customerId>, "status": <status> })
     * Index: { customerId: 1, status: 1, orderDate: -1 }
     *
     * @param customerId The customer ID
     * @param status The order status
     * @return List of orders matching both criteria
     */
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    /**
     * Find all orders for a customer with pagination.
     *
     * Query: db.orders.find({ "customerId": <customerId> }).limit().skip()
     * Index: { customerId: 1, orderDate: -1 }
     *
     * @param customerId The customer ID
     * @param pageable Pagination parameters
     * @return Page of orders for the customer
     */
    Page<Order> findByCustomerId(String customerId, Pageable pageable);
}

