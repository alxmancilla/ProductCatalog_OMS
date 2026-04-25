package com.example.store.service;

import com.example.store.validation.ProductValidationException;
import com.example.store.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Product validation using Strategy Pattern
 */
@SpringBootTest
class ProductValidationServiceTest {

    @Autowired
    private ProductValidationService validationService;

    @Test
    void shouldValidateElectronicsProductSuccessfully() {
        // Given: Valid electronics product
        Product product = new Product();
        product.setSku("ELEC-001");
        product.setName("Test Laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setInventory(10);
        product.setProductType(Product.ProductType.ELECTRONICS);
        product.setCategory("Laptops");
        product.setSchemaVersion(2);
        
        Product.ElectronicsDetails details = new Product.ElectronicsDetails();
        details.setBrand("TestBrand");
        details.setWarranty("12 months");
        product.setElectronicsDetails(details);

        // When/Then: Should not throw exception
        assertThatCode(() -> validationService.validateProduct(product))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailElectronicsWithoutBrand() {
        // Given: Electronics without brand
        Product product = new Product();
        product.setSku("ELEC-002");
        product.setName("Test Laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setInventory(10);
        product.setProductType(Product.ProductType.ELECTRONICS);
        product.setCategory("Laptops");
        product.setSchemaVersion(2);
        
        Product.ElectronicsDetails details = new Product.ElectronicsDetails();
        details.setWarranty("12 months");
        // Missing brand!
        product.setElectronicsDetails(details);

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> validationService.validateProduct(product))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("brand");
    }

    @Test
    void shouldValidateClothingProductSuccessfully() {
        // Given: Valid clothing product
        Product product = new Product();
        product.setSku("CLOTH-001");
        product.setName("Test Shirt");
        product.setPrice(new BigDecimal("49.99"));
        product.setInventory(50);
        product.setProductType(Product.ProductType.CLOTHING);
        product.setCategory("Shirts");
        product.setSchemaVersion(2);
        
        Product.ClothingDetails details = new Product.ClothingDetails();
        details.setSize("L");
        details.setColor("Blue");
        details.setMaterial("Cotton");
        product.setClothingDetails(details);

        // When/Then: Should not throw exception
        assertThatCode(() -> validationService.validateProduct(product))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailClothingWithoutSize() {
        // Given: Clothing without size
        Product product = new Product();
        product.setSku("CLOTH-002");
        product.setName("Test Shirt");
        product.setPrice(new BigDecimal("49.99"));
        product.setInventory(50);
        product.setProductType(Product.ProductType.CLOTHING);
        product.setCategory("Shirts");
        product.setSchemaVersion(2);
        
        Product.ClothingDetails details = new Product.ClothingDetails();
        details.setColor("Blue");
        details.setMaterial("Cotton");
        // Missing size!
        product.setClothingDetails(details);

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> validationService.validateProduct(product))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("size");
    }

    @Test
    void shouldValidateBookProductSuccessfully() {
        // Given: Valid book product
        Product product = new Product();
        product.setSku("BOOK-001");
        product.setName("Test Book");
        product.setPrice(new BigDecimal("29.99"));
        product.setInventory(100);
        product.setProductType(Product.ProductType.BOOK);
        product.setCategory("Technology");
        product.setSchemaVersion(2);
        
        Product.BookDetails details = new Product.BookDetails();
        details.setAuthor("Test Author");
        details.setIsbn("978-0123456789");
        details.setPublisher("Test Publisher");
        details.setPages(300);
        product.setBookDetails(details);

        // When/Then: Should not throw exception
        assertThatCode(() -> validationService.validateProduct(product))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailBookWithInvalidISBN() {
        // Given: Book with invalid ISBN
        Product product = new Product();
        product.setSku("BOOK-002");
        product.setName("Test Book");
        product.setPrice(new BigDecimal("29.99"));
        product.setInventory(100);
        product.setProductType(Product.ProductType.BOOK);
        product.setCategory("Technology");
        product.setSchemaVersion(2);
        
        Product.BookDetails details = new Product.BookDetails();
        details.setAuthor("Test Author");
        details.setIsbn("123"); // Invalid!
        details.setPublisher("Test Publisher");
        details.setPages(300);
        product.setBookDetails(details);

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> validationService.validateProduct(product))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    void shouldValidateGenericProductSuccessfully() {
        // Given: Valid generic product (no special details required)
        Product product = new Product();
        product.setSku("GEN-001");
        product.setName("Test Item");
        product.setPrice(new BigDecimal("19.99"));
        product.setInventory(75);
        product.setProductType(Product.ProductType.GENERIC);
        product.setCategory("Miscellaneous");
        product.setSchemaVersion(2);

        // When/Then: Should not throw exception
        assertThatCode(() -> validationService.validateProduct(product))
                .doesNotThrowAnyException();
    }
}
