package com.example.store.service.analytics;

import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderStatus;
import com.example.store.model.analytics.CustomerSpendingSummary;
import com.example.store.model.analytics.DailyRevenueSummary;
import com.example.store.model.analytics.ProductPopularitySummary;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.analytics.CustomerSpendingSummaryRepository;
import com.example.store.repository.analytics.DailyRevenueSummaryRepository;
import com.example.store.repository.analytics.ProductPopularitySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * CQRS Service: Updates Materialized Views when Orders Change
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Keep Read Models in Sync with Write Models
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service updates the materialized views (read models) whenever
 * an order is created, updated, or cancelled.
 * 
 * CQRS Flow:
 * 1. Write: Order created/updated in orders collection
 * 2. Event: This service is called
 * 3. Update: Materialized views are updated
 * 4. Read: Analytics queries use materialized views (fast!)
 * 
 * Eventually Consistent:
 * - Read models might be slightly behind write model (milliseconds)
 * - Acceptable for analytics use case
 * - Much faster than real-time aggregations
 * 
 * Update Strategies:
 * - Synchronous: Called immediately after order operation (current)
 * - Asynchronous: Via Change Streams or event bus (future enhancement)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializedViewUpdaterService {

    private final DailyRevenueSummaryRepository dailyRevenueRepository;
    private final ProductPopularitySummaryRepository productPopularityRepository;
    private final CustomerSpendingSummaryRepository customerSpendingRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    /**
     * Update all materialized views after order creation
     * 
     * @param order The newly created order
     */
    @Transactional
    public void onOrderCreated(Order order) {
        log.debug("Updating materialized views for new order: {}", order.getId());
        
        updateDailyRevenueSummary(order, true);
        updateProductPopularitySummary(order, true);
        updateCustomerSpendingSummary(order, true);
    }

    /**
     * Update all materialized views after order status change
     * 
     * @param order The updated order
     * @param oldStatus The previous status
     */
    @Transactional
    public void onOrderStatusChanged(Order order, OrderStatus oldStatus) {
        log.debug("Updating materialized views for order status change: {} {} -> {}", 
            order.getId(), oldStatus, order.getStatus());
        
        // Remove from old status bucket, add to new status bucket
        updateDailyRevenueSummary(order, false);
        updateCustomerSpendingSummary(order, false);
    }

    /**
     * Update all materialized views after order cancellation
     * 
     * @param order The cancelled order
     */
    @Transactional
    public void onOrderCancelled(Order order) {
        log.debug("Updating materialized views for cancelled order: {}", order.getId());
        
        // Reverse the additions
        updateDailyRevenueSummary(order, false);
        updateProductPopularitySummary(order, false);
        updateCustomerSpendingSummary(order, false);
    }

    /**
     * Update daily revenue summary
     */
    private void updateDailyRevenueSummary(Order order, boolean increment) {
        LocalDate orderDate = order.getOrderDate().toLocalDate();
        String status = order.getStatus().toString();
        String id = DailyRevenueSummary.createId(orderDate, status);

        DailyRevenueSummary summary = dailyRevenueRepository.findById(id)
            .orElseGet(() -> {
                DailyRevenueSummary newSummary = new DailyRevenueSummary();
                newSummary.setId(id);
                newSummary.setDate(orderDate);
                newSummary.setStatus(status);
                newSummary.setTotalRevenue(BigDecimal.ZERO);
                newSummary.setOrderCount(0);
                newSummary.setRevenueByHour(new HashMap<>());
                newSummary.setSchemaVersion(1);
                return newSummary;
            });

        // Update totals
        int delta = increment ? 1 : -1;
        BigDecimal revenueDelta = increment ? order.getTotal() : order.getTotal().negate();

        summary.setOrderCount(summary.getOrderCount() + delta);
        summary.setTotalRevenue(summary.getTotalRevenue().add(revenueDelta));

        // Update hourly breakdown
        int hour = order.getOrderDate().getHour();
        BigDecimal currentHourRevenue = summary.getRevenueByHour()
            .getOrDefault(hour, BigDecimal.ZERO);
        summary.getRevenueByHour().put(hour, currentHourRevenue.add(revenueDelta));

        summary.calculateAverageOrderValue();
        summary.setLastUpdated(LocalDateTime.now());

        dailyRevenueRepository.save(summary);
        log.debug("Updated daily revenue summary: {}", id);
    }

    /**
     * Update product popularity summary
     */
    private void updateProductPopularitySummary(Order order, boolean increment) {
        if (order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            updateProductStats(item, increment);
        }
    }

    private void updateProductStats(OrderItem item, boolean increment) {
        ProductPopularitySummary summary = productPopularityRepository
            .findById(item.getProductId())
            .orElseGet(() -> createNewProductSummary(item.getProductId()));

        int qtyDelta = increment ? item.getQuantity() : -item.getQuantity();
        BigDecimal revenueDelta = increment ? 
            item.getPrice().multiply(new BigDecimal(item.getQuantity())) :
            item.getPrice().multiply(new BigDecimal(item.getQuantity())).negate();

        summary.setTotalQuantitySold(
            (summary.getTotalQuantitySold() != null ? summary.getTotalQuantitySold() : 0) + qtyDelta
        );
        summary.setTotalRevenue(
            (summary.getTotalRevenue() != null ? summary.getTotalRevenue() : BigDecimal.ZERO).add(revenueDelta)
        );
        summary.setOrderCount(
            (summary.getOrderCount() != null ? summary.getOrderCount() : 0) + (increment ? 1 : -1)
        );

        summary.calculateDerivedFields();
        summary.setLastUpdated(LocalDateTime.now());

        productPopularityRepository.save(summary);
    }

    private ProductPopularitySummary createNewProductSummary(String productId) {
        ProductPopularitySummary summary = new ProductPopularitySummary();
        summary.setProductId(productId);
        
        // Denormalize product info
        productRepository.findById(productId).ifPresent(product -> {
            summary.setProductName(product.getName());
            summary.setCategory(product.getCategory());
        });
        
        summary.setTotalQuantitySold(0);
        summary.setTotalRevenue(BigDecimal.ZERO);
        summary.setOrderCount(0);
        summary.setSchemaVersion(1);
        
        return summary;
    }

    /**
     * Update customer spending summary
     */
    private void updateCustomerSpendingSummary(Order order, boolean increment) {
        CustomerSpendingSummary summary = customerSpendingRepository
            .findById(order.getCustomerId())
            .orElseGet(() -> createNewCustomerSummary(order.getCustomerId()));

        int orderDelta = increment ? 1 : -1;
        BigDecimal spentDelta = increment ? order.getTotal() : order.getTotal().negate();

        summary.setTotalOrders(
            (summary.getTotalOrders() != null ? summary.getTotalOrders() : 0) + orderDelta
        );

        if (order.getStatus() == OrderStatus.DELIVERED) {
            summary.setCompletedOrders(
                (summary.getCompletedOrders() != null ? summary.getCompletedOrders() : 0) + orderDelta
            );
            summary.setTotalSpent(
                (summary.getTotalSpent() != null ? summary.getTotalSpent() : BigDecimal.ZERO).add(spentDelta)
            );
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            summary.setCancelledOrders(
                (summary.getCancelledOrders() != null ? summary.getCancelledOrders() : 0) + orderDelta
            );
        }

        // Update dates
        LocalDate orderDate = order.getOrderDate().toLocalDate();
        if (summary.getFirstOrderDate() == null || orderDate.isBefore(summary.getFirstOrderDate())) {
            summary.setFirstOrderDate(orderDate);
        }
        if (summary.getLastOrderDate() == null || orderDate.isAfter(summary.getLastOrderDate())) {
            summary.setLastOrderDate(orderDate);
        }

        summary.calculateDerivedFields();
        summary.setLastUpdated(LocalDateTime.now());

        customerSpendingRepository.save(summary);
    }

    private CustomerSpendingSummary createNewCustomerSummary(String customerId) {
        CustomerSpendingSummary summary = new CustomerSpendingSummary();
        summary.setCustomerId(customerId);
        
        // Denormalize customer info
        customerRepository.findById(customerId).ifPresent(customer -> {
            summary.setCustomerName(customer.getName());
            summary.setTier(customer.getTier() != null ? customer.getTier().toString() : null);
        });
        
        summary.setTotalSpent(BigDecimal.ZERO);
        summary.setTotalOrders(0);
        summary.setCompletedOrders(0);
        summary.setCancelledOrders(0);
        summary.setSchemaVersion(1);
        
        return summary;
    }
}
