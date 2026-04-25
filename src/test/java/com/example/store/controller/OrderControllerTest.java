package com.example.store.controller;

import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.OrderStatus;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderItemBucketRepository;
import com.example.store.service.OrderCancellationService;
import com.example.store.service.OrderStatusService;
import com.example.store.service.OrderTransactionService;
import com.example.store.service.OrderUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for OrderController
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderItemBucketRepository orderItemBucketRepository;

    @MockBean
    private OrderTransactionService orderTransactionService;

    @MockBean
    private OrderStatusService orderStatusService;

    @MockBean
    private OrderCancellationService orderCancellationService;

    @MockBean
    private OrderUpdateService orderUpdateService;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given: Order request
        Order order = createTestOrder();
        Order savedOrder = createTestOrder();
        savedOrder.setId("ORDER-123");

        when(orderTransactionService.createOrderWithInventoryUpdate(any(Order.class)))
                .thenReturn(savedOrder);

        // When/Then: POST /orders
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORDER-123"))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        // Given: Order exists
        Order order = createTestOrder();
        order.setId("ORDER-123");

        when(orderRepository.findById("ORDER-123"))
                .thenReturn(Optional.of(order));

        // When/Then: GET /orders/{id}
        mockMvc.perform(get("/orders/ORDER-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORDER-123"))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        // Given: Order doesn't exist
        when(orderRepository.findById("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // When/Then: GET /orders/{id}
        mockMvc.perform(get("/orders/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllOrdersWithPagination() throws Exception {
        // Given: Page of orders
        List<Order> orders = List.of(createTestOrder(), createTestOrder());
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 20), 2);

        when(orderRepository.findAll(any(PageRequest.class)))
                .thenReturn(page);

        // When/Then: GET /orders?page=0&size=20
        mockMvc.perform(get("/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldFilterOrdersByStatus() throws Exception {
        // Given: Orders with status DELIVERED
        List<Order> orders = List.of(createTestOrder());

        when(orderRepository.findByStatus(OrderStatus.DELIVERED))
                .thenReturn(orders);

        // When/Then: GET /orders/search/by-status?status=DELIVERED
        mockMvc.perform(get("/orders/search/by-status")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));
    }

    // Helper methods
    private Order createTestOrder() {
        Order order = new Order();
        order.setCustomerId("CUST-123");
        order.setCustomerName("Test Customer");
        order.setStatus(OrderStatus.DELIVERED);
        order.setOrderDate(LocalDateTime.now());
        order.setTotal(new BigDecimal("999.99"));
        order.setSchemaVersion(4);

        OrderItem item = new OrderItem();
        item.setProductId("PROD-123");
        item.setName("Test Product");
        item.setPrice(new BigDecimal("999.99"));
        item.setQuantity(1);

        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setItems(items);

        return order;
    }
}
