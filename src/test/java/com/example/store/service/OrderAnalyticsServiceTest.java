package com.example.store.service;

import com.example.store.dto.PopularProductDTO;
import com.example.store.dto.RevenueByStatusDTO;
import com.example.store.dto.TopCustomerDTO;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderStatus;
import com.example.store.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OrderAnalyticsService aggregation pipelines
 */
@DataMongoTest
@Import(OrderAnalyticsService.class)
class OrderAnalyticsServiceTest {

    @Autowired
    private OrderAnalyticsService analyticsService;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/product_catalog_test");
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldCalculateRevenueByStatus() {
        // Given: Orders with different statuses
        createOrder("CUST-1", "Customer 1", OrderStatus.DELIVERED, new BigDecimal("100.00"));
        createOrder("CUST-2", "Customer 2", OrderStatus.DELIVERED, new BigDecimal("200.00"));
        createOrder("CUST-3", "Customer 3", OrderStatus.SHIPPED, new BigDecimal("150.00"));
        createOrder("CUST-4", "Customer 4", OrderStatus.CANCELLED, new BigDecimal("50.00"));

        // When: Get revenue by status
        List<RevenueByStatusDTO> result = analyticsService.getRevenueByStatus();

        // Then: Should have correct revenue per status (excluding CANCELLED)
        assertThat(result).hasSize(2);

        RevenueByStatusDTO delivered = result.stream()
                .filter(r -> r.getStatus() == OrderStatus.DELIVERED)
                .findFirst()
                .orElseThrow();

        assertThat(delivered.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(delivered.getOrderCount()).isEqualTo(2);
        assertThat(delivered.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void shouldFindTopCustomers() {
        // Given: Customers with different order totals
        createOrder("CUST-1", "Alice", OrderStatus.DELIVERED, new BigDecimal("500.00"));
        createOrder("CUST-1", "Alice", OrderStatus.DELIVERED, new BigDecimal("300.00"));
        createOrder("CUST-2", "Bob", OrderStatus.DELIVERED, new BigDecimal("700.00"));
        createOrder("CUST-3", "Charlie", OrderStatus.DELIVERED, new BigDecimal("100.00"));

        // When: Get top 2 customers
        List<TopCustomerDTO> result = analyticsService.getTopCustomers(2);

        // Then: Should return top 2 by spending
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice");
        assertThat(result.get(0).getTotalSpent()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(result.get(0).getOrderCount()).isEqualTo(2);

        assertThat(result.get(1).getCustomerName()).isEqualTo("Bob");
        assertThat(result.get(1).getTotalSpent()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    void shouldFindPopularProducts() {
        // Given: Orders with different products
        Order order1 = createOrderWithItems("CUST-1", "Customer 1", OrderStatus.DELIVERED,
                List.of(
                        createItem("PROD-1", "Laptop", new BigDecimal("1000.00"), 2),
                        createItem("PROD-2", "Mouse", new BigDecimal("25.00"), 1)
                ));

        Order order2 = createOrderWithItems("CUST-2", "Customer 2", OrderStatus.DELIVERED,
                List.of(
                        createItem("PROD-1", "Laptop", new BigDecimal("1000.00"), 1),
                        createItem("PROD-3", "Keyboard", new BigDecimal("75.00"), 2)
                ));

        orderRepository.saveAll(List.of(order1, order2));

        // When: Get popular products
        List<PopularProductDTO> result = analyticsService.getPopularProducts(10);

        // Then: Laptop should be #1 (3 units sold)
        assertThat(result).isNotEmpty();
        PopularProductDTO topProduct = result.get(0);
        assertThat(topProduct.getProductId()).isEqualTo("PROD-1");
        assertThat(topProduct.getTotalQuantitySold()).isEqualTo(3);
        assertThat(topProduct.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    // Helper methods
    private Order createOrder(String customerId, String customerName, OrderStatus status, BigDecimal total) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerName(customerName);
        order.setStatus(status);
        order.setTotal(total);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        order.setSchemaVersion(4);
        return orderRepository.save(order);
    }

    private Order createOrderWithItems(String customerId, String customerName, OrderStatus status, List<OrderItem> items) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerName(customerName);
        order.setStatus(status);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(items);
        order.setSchemaVersion(4);

        // Calculate total
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        return order;
    }

    private OrderItem createItem(String productId, String name, BigDecimal price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setName(name);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
