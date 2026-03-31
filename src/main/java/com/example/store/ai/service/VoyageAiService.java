package com.example.store.ai.service;

import com.example.store.ai.config.VoyageAiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with Voyage AI API.
 * Generates embeddings for product descriptions and search queries.
 */
@Service
public class VoyageAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoyageAiService.class);
    
    private final VoyageAiConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public VoyageAiService(VoyageAiConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        
        // Build WebClient for Voyage AI API
        this.webClient = WebClient.builder()
                .baseUrl(config.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + config.getKey())
                .build();
        
        logger.info("Voyage AI Service initialized with model: {}", config.getModel());
    }
    
    /**
     * Generate embedding for a single text.
     *
     * @param text Text to embed (product description or search query)
     * @param inputType "document" for indexing, "query" for searching
     * @return Embedding vector (List of Doubles)
     */
    public List<Double> generateEmbedding(String text, String inputType) {
        return generateEmbeddings(List.of(text), inputType).get(0);
    }
    
    /**
     * Generate embeddings for multiple texts (batch processing).
     *
     * @param texts List of texts to embed
     * @param inputType "document" for indexing, "query" for searching
     * @return List of embedding vectors
     */
    public List<List<Double>> generateEmbeddings(List<String> texts, String inputType) {
        try {
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("input_type", inputType);
            
            // Add input texts
            ArrayNode inputArray = requestBody.putArray("input");
            for (String text : texts) {
                inputArray.add(text);
            }
            
            logger.debug("Sending embedding request to Voyage AI: {} texts", texts.size());
            
            // Make API call
            String response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                logger.error("Voyage AI error response: {}", errorBody);
                                return Mono.error(new RuntimeException("Voyage AI error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .block();
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(response);
            JsonNode dataArray = responseJson.path("data");
            
            List<List<Double>> embeddings = new ArrayList<>();
            for (JsonNode dataNode : dataArray) {
                JsonNode embeddingArray = dataNode.path("embedding");
                List<Double> embedding = new ArrayList<>();
                for (JsonNode value : embeddingArray) {
                    embedding.add(value.asDouble());
                }
                embeddings.add(embedding);
            }
            
            logger.info("Generated {} embeddings (dimension: {})", 
                    embeddings.size(), 
                    embeddings.isEmpty() ? 0 : embeddings.get(0).size());
            
            return embeddings;
            
        } catch (Exception e) {
            logger.error("Error calling Voyage AI", e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate embedding for a product description (for indexing).
     *
     * @param description Product description
     * @return Embedding vector
     */
    public List<Double> generateProductEmbedding(String description) {
        return generateEmbedding(description, "document");
    }
    
    /**
     * Generate embedding for a search query.
     *
     * @param query Search query
     * @return Embedding vector
     */
    public List<Double> generateQueryEmbedding(String query) {
        return generateEmbedding(query, "query");
    }
}

