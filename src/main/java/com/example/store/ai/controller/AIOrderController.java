package com.example.store.ai.controller;

import com.example.store.ai.graph.OrderAssistantGraph;
import com.example.store.ai.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for AI-powered natural language order creation.
 * 
 * This endpoint demonstrates:
 * - Natural language processing with Grove API (GPT-5.4)
 * - LangGraph state machine for workflow orchestration
 * - All 7 MongoDB design patterns in action
 * - Fuzzy product matching
 * - Automatic customer resolution
 * 
 * Example requests:
 * - "I want 2 laptops for John Doe"
 * - "Order 3 blue t-shirts size L for Jane Smith"
 * - "Get me the MongoDB Guide book"
 */
@RestController
@RequestMapping("/ai")
public class AIOrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIOrderController.class);
    
    private final OrderAssistantGraph orderAssistantGraph;
    
    @Autowired
    public AIOrderController(OrderAssistantGraph orderAssistantGraph) {
        this.orderAssistantGraph = orderAssistantGraph;
    }
    
    /**
     * Create an order from natural language.
     * POST /ai/order
     * 
     * Request body:
     * {
     *   "message": "I want 2 laptops for John Doe"
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "✅ Great! I've created order #12345...",
     *   "orderId": "12345",
     *   "customerId": "cust123",
     *   "customerName": "John Doe",
     *   "total": "2599.98",
     *   "itemCount": 2
     * }
     */
    @PostMapping("/order")
    public ResponseEntity<AgentResponse> createOrderFromNaturalLanguage(
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Please provide a message with your order request.");
            errorResponse.setError("Missing 'message' field in request body");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.info("Received natural language order request: {}", message);
        
        try {
            // Execute the LangGraph workflow
            AgentResponse response = orderAssistantGraph.execute(message);
            
            if (response.isSuccess()) {
                logger.info("Successfully created order: {}", response.getOrderId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Failed to create order: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error processing order request", e);
            
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("I'm sorry, I encountered an unexpected error processing your order.");
            errorResponse.setError(e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the AI service.
     * GET /ai/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "AI Order Assistant",
                "model", "gpt-5.4 via Grove API"
        ));
    }
}

