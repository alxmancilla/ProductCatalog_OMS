package com.example.store.service;

import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for bulk-importing products from a JSON file.
 *
 * Skips products whose SKU already exists in the database (idempotent).
 * Returns a summary of how many were imported, skipped, and any per-product errors.
 */
@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public record ImportResult(int imported, int skipped, List<String> errors) {}

    /**
     * Parse a JSON array of products from the given stream and persist each one.
     * Products whose SKU is already in the database are skipped without error.
     */
    public ImportResult importProducts(InputStream jsonStream) throws IOException {
        List<Product> products = objectMapper.readValue(jsonStream, new TypeReference<List<Product>>() {});

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Product product : products) {
            try {
                String sku = product.getSku();
                if (sku != null && productRepository.findBySku(sku).isPresent()) {
                    skipped++;
                } else {
                    product.setId(null); // let MongoDB generate the _id
                    productRepository.save(product);
                    imported++;
                }
            } catch (Exception e) {
                errors.add("SKU " + product.getSku() + ": " + e.getMessage());
            }
        }

        return new ImportResult(imported, skipped, errors);
    }
}
