package com.example.store.ai.model;

import com.example.store.model.Customer;
import com.example.store.model.Order;
import com.example.store.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the AI agent as it processes an order request.
 * This state flows through all nodes in the LangGraph state machine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    
    // ═══════════════════════════════════════════════════════════════════════
    // INPUT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Original natural language input from the user.
     */
    private String userInput;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PARSED INTENT (from Intent Parser Node)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Structured intent parsed from natural language.
     */
    private OrderIntent intent;
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESOLVED ENTITIES (from Resolver Nodes)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Resolved customer (found or created).
     */
    private Customer customer;
    
    /**
     * Resolved products with their matched database entries.
     */
    private List<ResolvedProduct> resolvedProducts = new ArrayList<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CREATED ORDER (from Order Creator Node)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * The created order.
     */
    private Order order;
    
    // ═══════════════════════════════════════════════════════════════════════
    // OUTPUT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Natural language response to the user.
     */
    private String response;
    
    /**
     * Whether the order was successfully created.
     */
    private boolean success = false;
    
    /**
     * Error message if something went wrong.
     */
    private String errorMessage;
    
    /**
     * Represents a product matched from the database.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolvedProduct {
        /**
         * The original product request from the user.
         */
        private OrderIntent.ProductRequest request;
        
        /**
         * The matched product from the database.
         */
        private Product product;
        
        /**
         * Confidence score of the match (0.0 to 1.0).
         */
        private double matchScore;
        
        /**
         * Quantity to order.
         */
        private Integer quantity;
    }
}

