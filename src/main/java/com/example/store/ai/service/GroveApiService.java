package com.example.store.ai.service;

import com.example.store.ai.config.GroveApiConfig;
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
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Grove API (OpenAI gateway).
 * Handles chat completions using GPT models.
 */
@Service
public class GroveApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroveApiService.class);
    
    private final GroveApiConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public GroveApiService(GroveApiConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        
        // Build WebClient for Grove API
        this.webClient = WebClient.builder()
                .baseUrl(config.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-key", config.getKey())
                .build();
        
        logger.info("Grove API Service initialized with model: {}", config.getModel());
    }
    
    /**
     * Send a chat completion request to Grove API.
     *
     * @param messages List of messages (role + content)
     * @return The assistant's response content
     */
    public String chatCompletion(List<Map<String, String>> messages) {
        try {
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());

            // GPT-5.4 and newer models use max_completion_tokens instead of max_tokens
            if (config.getModel().startsWith("gpt-5") || config.getModel().startsWith("o1")) {
                requestBody.put("max_completion_tokens", config.getMaxTokens());
            } else {
                requestBody.put("max_tokens", config.getMaxTokens());
            }
            
            // Add messages
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (Map<String, String> message : messages) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", message.get("role"));
                messageNode.put("content", message.get("content"));
            }
            
            logger.debug("Sending request to Grove API: {}", requestBody.toPrettyString());
            
            // Make API call
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                logger.error("Grove API error response: {}", errorBody);
                                return Mono.error(new RuntimeException("Grove API error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .block();
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(response);
            String content = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
            
            logger.debug("Received response from Grove API: {}", content);
            
            return content;
            
        } catch (Exception e) {
            logger.error("Error calling Grove API", e);
            throw new RuntimeException("Failed to call Grove API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send a simple user message and get a response.
     *
     * @param userMessage The user's message
     * @return The assistant's response
     */
    public String simpleChat(String userMessage) {
        return chatCompletion(List.of(
                Map.of("role", "user", "content", userMessage)
        ));
    }
    
    /**
     * Send a system prompt + user message and get a response.
     *
     * @param systemPrompt System instructions for the AI
     * @param userMessage The user's message
     * @return The assistant's response
     */
    public String chatWithSystem(String systemPrompt, String userMessage) {
        return chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
    }
}

