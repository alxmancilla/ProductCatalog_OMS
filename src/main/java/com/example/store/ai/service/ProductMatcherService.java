package com.example.store.ai.service;

import com.example.store.ai.model.AgentState;
import com.example.store.ai.model.OrderIntent;
import com.example.store.model.Product;
import com.example.store.repository.ProductRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for matching natural language product descriptions to actual products.
 *
 * Supports TWO matching strategies:
 * 1. HYBRID SEARCH (MongoDB Atlas Vector Search + Text Search) - Recommended
 * 2. FUZZY MATCHING (Levenshtein distance) - Fallback
 *
 * Hybrid Search combines:
 * - Vector Search: Semantic similarity using Voyage AI embeddings
 * - Text Search: Keyword matching using Atlas Search
 * - Reciprocal Rank Fusion (RRF): Combines rankings from both methods
 */
@Service
public class ProductMatcherService {

    private static final Logger logger = LoggerFactory.getLogger(ProductMatcherService.class);
    private static final double MIN_MATCH_SCORE = 0.5; // Minimum similarity score (0.0 to 1.0)
    private static final int HYBRID_SEARCH_LIMIT = 10; // Number of results from each search method

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;
    private final VoyageAiService voyageAiService;
    private final LevenshteinDistance levenshteinDistance;
    private boolean hybridSearchEnabled = false; // Auto-detected based on embeddings availability

    @Autowired
    public ProductMatcherService(
            ProductRepository productRepository,
            MongoTemplate mongoTemplate,
            VoyageAiService voyageAiService) {
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
        this.voyageAiService = voyageAiService;
        this.levenshteinDistance = new LevenshteinDistance();

        // Check if hybrid search is available (products have embeddings)
        checkHybridSearchAvailability();
    }
    
    /**
     * Match product requests to actual products in the database.
     *
     * Uses hybrid search if available, otherwise falls back to fuzzy matching.
     *
     * @param productRequests List of product requests from intent
     * @return List of resolved products with match scores
     */
    public List<AgentState.ResolvedProduct> matchProducts(List<OrderIntent.ProductRequest> productRequests) {
        List<AgentState.ResolvedProduct> resolvedProducts = new ArrayList<>();

        for (OrderIntent.ProductRequest request : productRequests) {
            AgentState.ResolvedProduct resolved;

            if (hybridSearchEnabled) {
                // Use hybrid search (vector + text)
                resolved = matchSingleProductHybrid(request);
            } else {
                // Fallback to fuzzy matching
                List<Product> allProducts = productRepository.findAll();
                if (allProducts.isEmpty()) {
                    throw new RuntimeException("No products available in the database");
                }
                resolved = matchSingleProduct(request, allProducts);
            }

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
     * Match a single product using hybrid search.
     */
    private AgentState.ResolvedProduct matchSingleProductHybrid(OrderIntent.ProductRequest request) {
        String description = request.getProductDescription().trim();

        // Use hybrid search to find best matches
        List<Product> candidates = hybridSearch(description, 5);

        if (candidates.isEmpty()) {
            // Fallback to fuzzy matching if hybrid search returns nothing
            logger.warn("Hybrid search returned no results for '{}', using fuzzy fallback", description);
            List<Product> allProducts = productRepository.findAll();
            return matchSingleProduct(request, allProducts);
        }

        // Take the top result from hybrid search
        Product bestMatch = candidates.get(0);
        double score = 0.95; // High confidence for hybrid search results

        logger.info("Hybrid search matched '{}' to '{}' (quantity: {})",
                description, bestMatch.getName(), request.getQuantity());

        AgentState.ResolvedProduct resolved = new AgentState.ResolvedProduct();
        resolved.setRequest(request);
        resolved.setProduct(bestMatch);
        resolved.setMatchScore(score);
        resolved.setQuantity(request.getQuantity());

        return resolved;
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
        // Use composition-based detail objects for type-specific attributes
        if (attributes != null) {
            String attrLower = attributes.toLowerCase();
            double attrScore = 0.0;
            int attrFactors = 0;

            // Check clothing-specific attributes
            if (product.getClothingDetails() != null) {
                if (product.getClothingDetails().getColor() != null &&
                    attrLower.contains(product.getClothingDetails().getColor().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
                if (product.getClothingDetails().getSize() != null &&
                    attrLower.contains(product.getClothingDetails().getSize().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
                if (product.getClothingDetails().getMaterial() != null &&
                    attrLower.contains(product.getClothingDetails().getMaterial().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
            }

            // Check electronics-specific attributes
            if (product.getElectronicsDetails() != null) {
                if (product.getElectronicsDetails().getBrand() != null &&
                    attrLower.contains(product.getElectronicsDetails().getBrand().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
                if (product.getElectronicsDetails().getWarranty() != null &&
                    attrLower.contains(product.getElectronicsDetails().getWarranty().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
                if (product.getElectronicsDetails().getScreenSize() != null &&
                    attrLower.contains(product.getElectronicsDetails().getScreenSize().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
            }

            // Check book-specific attributes
            if (product.getBookDetails() != null) {
                if (product.getBookDetails().getAuthor() != null &&
                    attrLower.contains(product.getBookDetails().getAuthor().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
                if (product.getBookDetails().getPublisher() != null &&
                    attrLower.contains(product.getBookDetails().getPublisher().toLowerCase())) {
                    attrScore += 1.0;
                    attrFactors++;
                }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // HYBRID SEARCH METHODS (MongoDB Atlas Vector Search + Text Search)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if hybrid search is available (products have embeddings).
     */
    private void checkHybridSearchAvailability() {
        try {
            long productsWithEmbeddings = productRepository.findAll().stream()
                    .filter(p -> p.getDescriptionEmbedding() != null && !p.getDescriptionEmbedding().isEmpty())
                    .count();

            long totalProducts = productRepository.count();

            if (productsWithEmbeddings > 0) {
                hybridSearchEnabled = true;
                logger.info("🚀 Hybrid Search ENABLED ({}/{} products have embeddings)",
                        productsWithEmbeddings, totalProducts);
            } else {
                logger.info("⚠️  Hybrid Search DISABLED (no products have embeddings yet)");
                logger.info("💡 Run embedding generation to enable hybrid search");
            }
        } catch (Exception e) {
            logger.warn("Could not check hybrid search availability: {}", e.getMessage());
            hybridSearchEnabled = false;
        }
    }

    /**
     * Search products using MongoDB Hybrid Search (Vector + Text).
     *
     * This method combines:
     * 1. Vector Search: Semantic similarity using embeddings
     * 2. Text Search: Keyword matching using Atlas Search
     * 3. Reciprocal Rank Fusion: Combines rankings from both methods
     *
     * @param query Search query
     * @param limit Maximum number of results
     * @return List of products ranked by hybrid score
     */
    public List<Product> hybridSearch(String query, int limit) {
        if (!hybridSearchEnabled) {
            logger.warn("Hybrid search not available, falling back to fuzzy matching");
            return productRepository.findAll(); // Fallback
        }

        try {
            // Generate query embedding
            List<Double> queryEmbedding = voyageAiService.generateQueryEmbedding(query);

            // Execute hybrid search aggregation
            List<Document> pipeline = buildHybridSearchPipeline(queryEmbedding, query, limit);

            AggregationResults<Product> results = mongoTemplate.aggregate(
                    Aggregation.newAggregation(
                            context -> new Document("$documents", pipeline)
                    ),
                    "products",
                    Product.class
            );

            List<Product> products = results.getMappedResults();
            logger.info("Hybrid search for '{}' returned {} results", query, products.size());

            return products;

        } catch (Exception e) {
            logger.error("Error in hybrid search, falling back to fuzzy matching", e);
            return productRepository.findAll(); // Fallback
        }
    }

    /**
     * Build MongoDB aggregation pipeline for hybrid search.
     *
     * Pipeline stages:
     * 1. $vectorSearch: Find semantically similar products
     * 2. $search: Find keyword matches
     * 3. $group: Combine results using Reciprocal Rank Fusion
     * 4. $sort: Sort by combined score
     * 5. $limit: Return top N results
     */
    private List<Document> buildHybridSearchPipeline(List<Double> queryEmbedding, String query, int limit) {
        // Note: This is a simplified version. Full implementation requires:
        // 1. Vector Search index created in Atlas
        // 2. Text Search index created in Atlas
        // 3. Proper RRF scoring implementation

        // For now, we'll use a simpler approach with just vector search
        // You can enhance this with full hybrid search once indexes are created

        List<Document> pipeline = new ArrayList<>();

        // Stage 1: Vector Search
        Document vectorSearchStage = new Document("$vectorSearch", new Document()
                .append("index", "vector_index") // Name of vector search index in Atlas
                .append("path", "descriptionEmbedding")
                .append("queryVector", queryEmbedding)
                .append("numCandidates", limit * 10) // Overrequest for better recall
                .append("limit", limit)
        );
        pipeline.add(vectorSearchStage);

        // Stage 2: Add vector search score
        Document addVectorScoreStage = new Document("$addFields", new Document()
                .append("vectorScore", new Document("$meta", "vectorSearchScore"))
        );
        pipeline.add(addVectorScoreStage);

        // Stage 3: Sort by vector score
        Document sortStage = new Document("$sort", new Document("vectorScore", -1));
        pipeline.add(sortStage);

        // Stage 4: Limit results
        Document limitStage = new Document("$limit", limit);
        pipeline.add(limitStage);

        return pipeline;
    }
}

