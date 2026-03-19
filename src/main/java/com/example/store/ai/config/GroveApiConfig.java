package com.example.store.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Grove API (OpenAI gateway).
 * Properties are loaded from application.properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "grove.api")
public class GroveApiConfig {
    
    /**
     * Grove API base URL.
     * Default: https://grove-gateway-prod.azure-api.net/grove-foundry-prod/openai/v1
     */
    private String url = "https://grove-gateway-prod.azure-api.net/grove-foundry-prod/openai/v1";
    
    /**
     * Grove API key for authentication.
     */
    private String key;
    
    /**
     * Model to use for completions.
     * Default: gpt-5.4
     */
    private String model = "gpt-5.4";
    
    /**
     * Temperature for LLM responses (0.0 to 1.0).
     * Lower = more deterministic, Higher = more creative.
     * Default: 0.7
     */
    private Double temperature = 0.7;
    
    /**
     * Maximum tokens in the response.
     * Default: 1000
     */
    private Integer maxTokens = 1000;
    
    /**
     * Timeout for API calls in seconds.
     * Default: 30
     */
    private Integer timeoutSeconds = 30;
}

