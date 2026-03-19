package com.example.store.ai.service;

import com.example.store.ai.model.AgentState;
import com.example.store.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for generating natural language responses to users.
 * Uses Grove API to create human-friendly confirmations.
 */
@Service
public class ResponseGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseGeneratorService.class);
    
    private final GroveApiService groveApiService;
    
    @Autowired
    public ResponseGeneratorService(GroveApiService groveApiService) {
        this.groveApiService = groveApiService;
    }
    
    /**
     * Generate a natural language confirmation response.
     *
     * @param order The created order
     * @param state The agent state with all context
     * @return Natural language response
     */
    public String generateSuccessResponse(Order order, AgentState state) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userContext = buildUserContext(order, state);
            
            String response = groveApiService.chatWithSystem(systemPrompt, userContext);
            
            logger.info("Generated success response for order {}", order.getId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating response, using fallback", e);
            return generateFallbackResponse(order, state);
        }
    }
    
    /**
     * Generate an error response.
     *
     * @param errorMessage The error message
     * @param userInput The original user input
     * @return Natural language error response
     */
    public String generateErrorResponse(String errorMessage, String userInput) {
        try {
            String systemPrompt = """
                    You are a helpful order assistant. The user's order request failed.
                    Explain the error in a friendly, helpful way and suggest what they can do.
                    Be concise and empathetic.
                    """;
            
            String userContext = String.format(
                    "User request: \"%s\"\nError: %s\n\nGenerate a friendly error message.",
                    userInput, errorMessage
            );
            
            return groveApiService.chatWithSystem(systemPrompt, userContext);
            
        } catch (Exception e) {
            logger.error("Error generating error response, using fallback", e);
            return "I'm sorry, I couldn't process your order request. " + errorMessage;
        }
    }
    
    /**
     * Build the system prompt for response generation.
     */
    private String buildSystemPrompt() {
        return """
                You are a friendly order confirmation assistant for an e-commerce system.
                Your job is to generate a natural, conversational confirmation message for a successfully created order.
                
                Guidelines:
                - Be warm and friendly
                - Confirm what was ordered
                - Include the order ID
                - Mention the total amount
                - Thank the customer
                - Keep it concise (2-3 sentences)
                - Use emojis sparingly (1-2 max)
                
                Example:
                "✅ Great! I've created order #12345 for John Doe with 2 laptops for a total of $2,599.98. Thank you for your order!"
                
                Now generate a confirmation message based on the order details provided.
                """;
    }
    
    /**
     * Build the user context with order details.
     */
    private String buildUserContext(Order order, AgentState state) {
        StringBuilder context = new StringBuilder();
        context.append("Order Details:\n");
        context.append("- Order ID: ").append(order.getId()).append("\n");
        context.append("- Customer: ").append(order.getCustomerName()).append("\n");
        context.append("- Total: $").append(order.getTotal()).append("\n");
        context.append("- Items:\n");
        
        for (AgentState.ResolvedProduct resolved : state.getResolvedProducts()) {
            context.append("  - ")
                    .append(resolved.getQuantity())
                    .append("x ")
                    .append(resolved.getProduct().getName())
                    .append(" @ $")
                    .append(resolved.getProduct().getPrice())
                    .append(" each\n");
        }
        
        if (order.getIsLargeOrder()) {
            context.append("- Note: This is a large order with ")
                    .append(order.getTotalItemCount())
                    .append(" items\n");
        }
        
        context.append("\nGenerate a friendly confirmation message.");
        
        return context.toString();
    }
    
    /**
     * Generate a fallback response if LLM fails.
     */
    private String generateFallbackResponse(Order order, AgentState state) {
        int itemCount = state.getResolvedProducts().size();
        String itemWord = itemCount == 1 ? "item" : "items";
        
        return String.format(
                "✅ Order created successfully! Order #%s for %s with %d %s. Total: $%s. Thank you!",
                order.getId(),
                order.getCustomerName(),
                itemCount,
                itemWord,
                order.getTotal()
        );
    }
}

