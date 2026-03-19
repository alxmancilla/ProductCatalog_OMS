package com.example.store.ai.service;

import com.example.store.ai.model.AgentState;
import com.example.store.ai.model.OrderIntent;
import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for matching natural language product descriptions to actual products.
 * Uses fuzzy matching to find the best product matches.
 */
@Service
public class ProductMatcherService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductMatcherService.class);
    private static final double MIN_MATCH_SCORE = 0.5; // Minimum similarity score (0.0 to 1.0)
    
    private final ProductRepository productRepository;
    private final LevenshteinDistance levenshteinDistance;
    
    @Autowired
    public ProductMatcherService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.levenshteinDistance = new LevenshteinDistance();
    }
    
    /**
     * Match product requests to actual products in the database.
     *
     * @param productRequests List of product requests from intent
     * @return List of resolved products with match scores
     */
    public List<AgentState.ResolvedProduct> matchProducts(List<OrderIntent.ProductRequest> productRequests) {
        List<AgentState.ResolvedProduct> resolvedProducts = new ArrayList<>();
        List<Product> allProducts = productRepository.findAll();
        
        if (allProducts.isEmpty()) {
            throw new RuntimeException("No products available in the database");
        }
        
        for (OrderIntent.ProductRequest request : productRequests) {
            AgentState.ResolvedProduct resolved = matchSingleProduct(request, allProducts);
            
            if (resolved.getMatchScore() < MIN_MATCH_SCORE) {
                logger.warn("Low confidence match for '{}': {} (score: {})", 
                        request.getProductDescription(), 
                        resolved.getProduct().getName(),
                        resolved.getMatchScore());
            }
            
            resolvedProducts.add(resolved);
        }
        
        return resolvedProducts;
    }
    
    /**
     * Match a single product request to the best matching product.
     */
    private AgentState.ResolvedProduct matchSingleProduct(
            OrderIntent.ProductRequest request, 
            List<Product> allProducts) {
        
        String description = request.getProductDescription().toLowerCase().trim();
        Product bestMatch = null;
        double bestScore = 0.0;
        
        for (Product product : allProducts) {
            double score = calculateMatchScore(description, product, request.getTypeHint(), request.getAttributes());
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = product;
            }
        }
        
        if (bestMatch == null) {
            // Fallback to first product if no match found
            bestMatch = allProducts.get(0);
            bestScore = 0.3;
            logger.warn("No good match found for '{}', using fallback: {}", 
                    description, bestMatch.getName());
        }
        
        logger.info("Matched '{}' to '{}' (score: {}, quantity: {})", 
                description, bestMatch.getName(), bestScore, request.getQuantity());
        
        AgentState.ResolvedProduct resolved = new AgentState.ResolvedProduct();
        resolved.setRequest(request);
        resolved.setProduct(bestMatch);
        resolved.setMatchScore(bestScore);
        resolved.setQuantity(request.getQuantity());
        
        return resolved;
    }
    
    /**
     * Calculate match score between description and product.
     * Returns a score between 0.0 (no match) and 1.0 (perfect match).
     *
     * 🆕 Enhanced to use product descriptions for better semantic matching!
     */
    private double calculateMatchScore(String description, Product product, String typeHint, String attributes) {
        double score = 0.0;
        int factors = 0;

        // 1. Name similarity (most important - weight: 2x)
        if (product.getName() != null) {
            String productName = product.getName().toLowerCase();

            // Exact match
            if (productName.equals(description)) {
                score += 2.0;  // Double weight for name
            }
            // Contains match
            else if (productName.contains(description) || description.contains(productName)) {
                score += 1.6;  // 0.8 * 2
            }
            // Fuzzy match using Levenshtein distance
            else {
                double similarity = calculateStringSimilarity(description, productName);
                score += similarity * 2.0;  // Double weight
            }
            factors += 2;  // Count as 2 factors due to double weight
        }

        // 2. 🆕 Description similarity (helps with semantic matching)
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            String productDesc = product.getDescription().toLowerCase();

            // Check if search term appears in description
            if (productDesc.contains(description)) {
                score += 0.9;
            }
            // Check for individual words in description
            else {
                String[] searchWords = description.split("\\s+");
                int matchedWords = 0;
                for (String word : searchWords) {
                    if (word.length() > 2 && productDesc.contains(word)) {
                        matchedWords++;
                    }
                }
                if (searchWords.length > 0) {
                    score += (double) matchedWords / searchWords.length * 0.7;
                }
            }
            factors++;
        }

        // 3. Category match
        if (product.getCategory() != null) {
            String category = product.getCategory().toLowerCase();
            if (description.contains(category) || category.contains(description)) {
                score += 0.6;
                factors++;
            }
        }

        // 4. Type hint match
        if (typeHint != null && product.getType() != null) {
            if (product.getType().equalsIgnoreCase(typeHint)) {
                score += 1.0;
            }
            factors++;
        }

        // 5. Attributes match (color, size, brand, etc.)
        if (attributes != null) {
            String attrLower = attributes.toLowerCase();
            double attrScore = 0.0;
            int attrFactors = 0;

            if (product.getColor() != null && attrLower.contains(product.getColor().toLowerCase())) {
                attrScore += 1.0;
                attrFactors++;
            }
            if (product.getSize() != null && attrLower.contains(product.getSize().toLowerCase())) {
                attrScore += 1.0;
                attrFactors++;
            }
            if (product.getBrand() != null && attrLower.contains(product.getBrand().toLowerCase())) {
                attrScore += 1.0;
                attrFactors++;
            }
            if (product.getMaterial() != null && attrLower.contains(product.getMaterial().toLowerCase())) {
                attrScore += 1.0;
                attrFactors++;
            }

            if (attrFactors > 0) {
                score += (attrScore / attrFactors);
                factors++;
            }
        }

        // Return weighted average score
        return factors > 0 ? score / factors : 0.0;
    }
    
    /**
     * Calculate string similarity using Levenshtein distance.
     * Returns a score between 0.0 (completely different) and 1.0 (identical).
     */
    private double calculateStringSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance.apply(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }
}

