package com.example.store.service;

import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.Product;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for OrderTransactionService
 * Tests ACID transaction behavior and inventory management
 */
@DataMongoTest
@Import(OrderTransactionService.class)
class OrderTransactionServiceTest {

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        // Use local MongoDB for tests
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/product_catalog_test");
    }

    @BeforeEach
    void setUp() {
        // Clean up before each test
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndDecrementInventory() {
        // Given: Product with inventory 10
        Product product = createProduct("SKU-001", "Test Laptop", new BigDecimal("999.99"), 10);
        product = productRepository.save(product);

        // When: Create order for 3 units
        Order order = createOrder(product.getId(), "Test Laptop", new BigDecimal("999.99"), 3);
        Order savedOrder = orderTransactionService.createOrderWithInventoryUpdate(order);

        // Then: Order created
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("2999.97"));

        // And: Inventory decremented atomically
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getInventory()).isEqualTo(7);
    }

    @Test
    void shouldRollbackOnInsufficientInventory() {
        // Given: Product with inventory 2
        Product product = createProduct("SKU-002", "Test Mouse", new BigDecimal("29.99"), 2);
        product = productRepository.save(product);

        // When: Try to order 5 units
        Order order = createOrder(product.getId(), "Test Mouse", new BigDecimal("29.99"), 5);

        // Then: Should throw exception
        assertThatThrownBy(() -> orderTransactionService.createOrderWithInventoryUpdate(order))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory");

        // And: Order should NOT be created
        assertThat(orderRepository.count()).isZero();

        // And: Inventory should still be 2 (transaction rolled back)
        Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchangedProduct.getInventory()).isEqualTo(2);
    }

    @Test
    void shouldHandleMultipleItemsInOrder() {
        // Given: Two products with inventory
        Product laptop = createProduct("SKU-003", "Laptop", new BigDecimal("1500.00"), 5);
        Product mouse = createProduct("SKU-004", "Mouse", new BigDecimal("25.00"), 10);
        laptop = productRepository.save(laptop);
        mouse = productRepository.save(mouse);

        // When: Order both products
        Order order = new Order();
        order.setCustomerId("CUST-001");
        order.setCustomerName("Test Customer");
        List<OrderItem> items = new ArrayList<>();
        items.add(createOrderItem(laptop.getId(), "Laptop", new BigDecimal("1500.00"), 2));
        items.add(createOrderItem(mouse.getId(), "Mouse", new BigDecimal("25.00"), 3));
        order.setItems(items);

        Order savedOrder = orderTransactionService.createOrderWithInventoryUpdate(order);

        // Then: Both inventories decremented
        Product updatedLaptop = productRepository.findById(laptop.getId()).orElseThrow();
        Product updatedMouse = productRepository.findById(mouse.getId()).orElseThrow();
        
        assertThat(updatedLaptop.getInventory()).isEqualTo(3);
        assertThat(updatedMouse.getInventory()).isEqualTo(7);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("3075.00"));
    }

    @Test
    void shouldRollbackAllItemsIfOneFailsInventoryCheck() {
        // Given: Laptop with sufficient inventory, Mouse with insufficient
        Product laptop = createProduct("SKU-005", "Laptop", new BigDecimal("1500.00"), 10);
        Product mouse = createProduct("SKU-006", "Mouse", new BigDecimal("25.00"), 2);
        laptop = productRepository.save(laptop);
        mouse = productRepository.save(mouse);

        // When: Order exceeds mouse inventory
        Order order = new Order();
        order.setCustomerId("CUST-002");
        order.setCustomerName("Test Customer");
        List<OrderItem> items = new ArrayList<>();
        items.add(createOrderItem(laptop.getId(), "Laptop", new BigDecimal("1500.00"), 1));
        items.add(createOrderItem(mouse.getId(), "Mouse", new BigDecimal("25.00"), 5)); // Exceeds!
        order.setItems(items);

        // Then: Should fail
        assertThatThrownBy(() -> orderTransactionService.createOrderWithInventoryUpdate(order))
                .isInstanceOf(InsufficientInventoryException.class);

        // And: BOTH inventories should remain unchanged (atomic rollback)
        Product unchangedLaptop = productRepository.findById(laptop.getId()).orElseThrow();
        Product unchangedMouse = productRepository.findById(mouse.getId()).orElseThrow();
        
        assertThat(unchangedLaptop.getInventory()).isEqualTo(10);
        assertThat(unchangedMouse.getInventory()).isEqualTo(2);
        
        // And: No order created
        assertThat(orderRepository.count()).isZero();
    }

    // Helper methods
    private Product createProduct(String sku, String name, BigDecimal price, int inventory) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setInventory(inventory);
        product.setProductType(Product.ProductType.GENERIC);
        product.setCategory("Test Category");
        product.setSchemaVersion(2);
        return product;
    }

    private Order createOrder(String productId, String productName, BigDecimal price, int quantity) {
        Order order = new Order();
        order.setCustomerId("CUST-TEST");
        order.setCustomerName("Test Customer");
        
        List<OrderItem> items = new ArrayList<>();
        items.add(createOrderItem(productId, productName, price, quantity));
        order.setItems(items);
        
        return order;
    }

    private OrderItem createOrderItem(String productId, String name, BigDecimal price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setName(name);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
