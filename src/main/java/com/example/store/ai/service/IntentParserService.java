package com.example.store.ai.service;

import com.example.store.ai.model.OrderIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing natural language into structured OrderIntent.
 * Uses Grove API (GPT) to extract customer and product information.
 */
@Service
public class IntentParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentParserService.class);
    
    private final GroveApiService groveApiService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public IntentParserService(GroveApiService groveApiService, ObjectMapper objectMapper) {
        this.groveApiService = groveApiService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Parse natural language order request into structured intent.
     *
     * @param userInput Natural language order request
     * @return Parsed OrderIntent
     */
    public OrderIntent parseIntent(String userInput) {
        try {
            String systemPrompt = buildSystemPrompt();
            String response = groveApiService.chatWithSystem(systemPrompt, userInput);
            
            logger.info("Parsed intent from user input: {}", userInput);
            logger.debug("LLM response: {}", response);
            
            // Parse JSON response into OrderIntent
            OrderIntent intent = objectMapper.readValue(response, OrderIntent.class);
            
            // Validate and set defaults
            if (intent.getProducts() == null) {
                intent.setProducts(new ArrayList<>());
            }
            
            // Set default quantities if not specified
            for (OrderIntent.ProductRequest product : intent.getProducts()) {
                if (product.getQuantity() == null || product.getQuantity() <= 0) {
                    product.setQuantity(1);
                }
            }
            
            return intent;
            
        } catch (Exception e) {
            logger.error("Error parsing intent from user input: {}", userInput, e);
            throw new RuntimeException("Failed to parse order intent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build the system prompt for intent parsing.
     */
    private String buildSystemPrompt() {
        return """
                You are an order intent parser for an e-commerce system.
                Your job is to extract structured information from natural language order requests.
                
                Extract the following information:
                1. Customer name (if mentioned)
                2. Customer email (if mentioned)
                3. Customer phone (if mentioned)
                4. List of products with:
                   - Product description (name or description)
                   - Quantity (default to 1 if not specified)
                   - Type hint (Electronics, Clothing, Book, etc. if mentioned)
                   - Attributes (color, size, brand, etc. if mentioned)
                
                Return ONLY valid JSON in this exact format:
                {
                  "customerName": "John Doe",
                  "customerEmail": "john@example.com",
                  "customerPhone": "+1-555-0123",
                  "products": [
                    {
                      "productDescription": "laptop",
                      "quantity": 2,
                      "typeHint": "Electronics",
                      "attributes": "brand: Dell, warranty: 2 years"
                    }
                  ]
                }
                
                Rules:
                - If customer info is not mentioned, omit those fields or set to null
                - Always include at least one product
                - Extract quantity from phrases like "2 laptops", "three books", etc.
                - Infer product type from context (laptop = Electronics, t-shirt = Clothing, etc.)
                - Capture attributes like color, size, brand in the attributes field
                - Return ONLY the JSON, no additional text or explanation
                
                Examples:
                
                Input: "I want 2 laptops for John Doe"
                Output: {"customerName":"John Doe","products":[{"productDescription":"laptop","quantity":2,"typeHint":"Electronics"}]}
                
                Input: "Order 3 blue t-shirts size L for Jane Smith, email jane@example.com"
                Output: {"customerName":"Jane Smith","customerEmail":"jane@example.com","products":[{"productDescription":"t-shirt","quantity":3,"typeHint":"Clothing","attributes":"color: blue, size: L"}]}
                
                Input: "Get me the MongoDB Guide book"
                Output: {"products":[{"productDescription":"MongoDB Guide","quantity":1,"typeHint":"Book"}]}
                
                Now parse the user's order request.
                """;
    }
}

