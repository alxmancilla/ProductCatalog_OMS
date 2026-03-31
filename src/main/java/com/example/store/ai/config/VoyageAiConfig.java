package com.example.store.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Voyage AI API (Embeddings for Hybrid Search).
 * Properties are loaded from application.properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "voyage.ai")
public class VoyageAiConfig {
    
    /**
     * Voyage AI API base URL.
     * Default: https://api.voyageai.com/v1
     */
    private String url = "https://api.voyageai.com/v1";
    
    /**
     * Voyage AI API key for authentication.
     */
    private String key;
    
    /**
     * Embedding model to use.
     * Default: voyage-4-large (optimized for retrieval)
     */
    private String model = "voyage-4-large";
    
    /**
     * Input type for embeddings.
     * - "document" for indexing product descriptions
     * - "query" for search queries
     * Default: document
     */
    private String inputType = "document";
    
    /**
     * Timeout for API calls in seconds.
     * Default: 30
     */
    private Integer timeoutSeconds = 30;
}

