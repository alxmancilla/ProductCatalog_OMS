package com.example.store.util;

import com.example.store.ai.service.VoyageAiService;
import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to generate embeddings for all products in the database.
 * 
 * This runs automatically on application startup if enabled via:
 * generate.embeddings.enabled=true
 * 
 * Usage:
 * 1. Set your Voyage AI API key in application.properties
 * 2. Enable embedding generation: generate.embeddings.enabled=true
 * 3. Start the application
 * 4. Embeddings will be generated for all products without embeddings
 * 5. Disable after first run: generate.embeddings.enabled=false
 */
@Component
@ConditionalOnProperty(name = "generate.embeddings.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmbeddingGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingGenerator.class);
    private static final int BATCH_SIZE = 10; // Process 10 products at a time

    private final ProductRepository productRepository;
    private final VoyageAiService voyageAiService;
    
    @Override
    public void run(String... args) {
        logger.info("🤖 Starting embedding generation for products...");
        
        try {
            // Find all products without embeddings
            List<Product> allProducts = productRepository.findAll();
            List<Product> productsNeedingEmbeddings = allProducts.stream()
                    .filter(p -> p.getDescriptionEmbedding() == null || p.getDescriptionEmbedding().isEmpty())
                    .toList();
            
            if (productsNeedingEmbeddings.isEmpty()) {
                logger.info("✅ All products already have embeddings!");
                return;
            }
            
            logger.info("Found {} products needing embeddings (out of {} total)", 
                    productsNeedingEmbeddings.size(), allProducts.size());
            
            int totalProcessed = 0;
            int totalBatches = (int) Math.ceil((double) productsNeedingEmbeddings.size() / BATCH_SIZE);
            
            // Process in batches
            for (int i = 0; i < productsNeedingEmbeddings.size(); i += BATCH_SIZE) {
                int batchNum = (i / BATCH_SIZE) + 1;
                int endIndex = Math.min(i + BATCH_SIZE, productsNeedingEmbeddings.size());
                List<Product> batch = productsNeedingEmbeddings.subList(i, endIndex);
                
                logger.info("Processing batch {}/{} ({} products)...", batchNum, totalBatches, batch.size());
                
                // Collect descriptions for batch
                List<String> descriptions = new ArrayList<>();
                for (Product product : batch) {
                    String description = product.getDescription();
                    if (description == null || description.trim().isEmpty()) {
                        // Fallback to name if description is missing
                        description = product.getName();
                    }
                    descriptions.add(description);
                }
                
                // Generate embeddings for batch
                List<List<Double>> embeddings = voyageAiService.generateEmbeddings(descriptions, "document");
                
                // Update products with embeddings
                for (int j = 0; j < batch.size(); j++) {
                    Product product = batch.get(j);
                    product.setDescriptionEmbedding(embeddings.get(j));
                    productRepository.save(product);
                    totalProcessed++;
                    
                    logger.debug("✅ Generated embedding for: {} (dimension: {})", 
                            product.getName(), embeddings.get(j).size());
                }
                
                logger.info("✅ Batch {}/{} complete ({}/{} products processed)", 
                        batchNum, totalBatches, totalProcessed, productsNeedingEmbeddings.size());
                
                // Small delay between batches to avoid rate limiting
                if (i + BATCH_SIZE < productsNeedingEmbeddings.size()) {
                    Thread.sleep(1000); // 1 second delay
                }
            }
            
            logger.info("🎉 Embedding generation complete! Processed {} products", totalProcessed);
            logger.info("💡 Remember to disable embedding generation: generate.embeddings.enabled=false");
            logger.info("💡 Next step: Create Vector Search index in MongoDB Atlas");
            
        } catch (Exception e) {
            logger.error("❌ Error generating embeddings", e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }
}

