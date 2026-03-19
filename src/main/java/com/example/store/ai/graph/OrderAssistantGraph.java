package com.example.store.ai.graph;

import com.example.store.ai.model.AgentResponse;
import com.example.store.ai.model.AgentState;
import com.example.store.ai.model.OrderIntent;
import com.example.store.ai.service.*;
import com.example.store.exception.InsufficientInventoryException;
import com.example.store.exception.InsufficientInventoryException.InventoryInfo;
import com.example.store.model.Customer;
import com.example.store.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LangGraph-style state machine for processing natural language order requests.
 * 
 * Flow:
 * 1. Parse Intent (natural language → structured data)
 * 2. Resolve Customer (find or create)
 * 3. Match Products (fuzzy matching)
 * 4. Create Order (with all MongoDB patterns)
 * 5. Generate Response (natural language confirmation)
 */
@Component
public class OrderAssistantGraph {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderAssistantGraph.class);
    
    private final IntentParserService intentParserService;
    private final CustomerResolverService customerResolverService;
    private final ProductMatcherService productMatcherService;
    private final OrderCreatorService orderCreatorService;
    private final ResponseGeneratorService responseGeneratorService;
    
    @Autowired
    public OrderAssistantGraph(
            IntentParserService intentParserService,
            CustomerResolverService customerResolverService,
            ProductMatcherService productMatcherService,
            OrderCreatorService orderCreatorService,
            ResponseGeneratorService responseGeneratorService) {
        this.intentParserService = intentParserService;
        this.customerResolverService = customerResolverService;
        this.productMatcherService = productMatcherService;
        this.orderCreatorService = orderCreatorService;
        this.responseGeneratorService = responseGeneratorService;
    }
    
    /**
     * Execute the complete order assistant workflow.
     *
     * @param userInput Natural language order request
     * @return Agent response with order details or error
     */
    public AgentResponse execute(String userInput) {
        logger.info("Starting order assistant workflow for input: {}", userInput);
        
        AgentState state = new AgentState();
        state.setUserInput(userInput);
        
        try {
            // Node 1: Parse Intent
            state = parseIntentNode(state);
            
            // Node 2: Resolve Customer
            state = resolveCustomerNode(state);
            
            // Node 3: Match Products
            state = matchProductsNode(state);
            
            // Node 4: Create Order
            state = createOrderNode(state);
            
            // Node 5: Generate Response
            state = generateResponseNode(state);
            
            // Build success response
            return buildSuccessResponse(state);
            
        } catch (Exception e) {
            logger.error("Error in order assistant workflow", e);
            return buildErrorResponse(state, e);
        }
    }
    
    /**
     * Node 1: Parse natural language into structured intent.
     */
    private AgentState parseIntentNode(AgentState state) {
        logger.info("Node 1: Parsing intent");
        
        OrderIntent intent = intentParserService.parseIntent(state.getUserInput());
        state.setIntent(intent);
        
        logger.info("Parsed intent: customer={}, products={}", 
                intent.getCustomerName(), 
                intent.getProducts().size());
        
        return state;
    }
    
    /**
     * Node 2: Resolve customer (find or create).
     */
    private AgentState resolveCustomerNode(AgentState state) {
        logger.info("Node 2: Resolving customer");
        
        Customer customer = customerResolverService.resolveCustomer(state.getIntent());
        state.setCustomer(customer);
        
        logger.info("Resolved customer: {} (ID: {})", customer.getName(), customer.getId());
        
        return state;
    }
    
    /**
     * Node 3: Match products using fuzzy matching.
     */
    private AgentState matchProductsNode(AgentState state) {
        logger.info("Node 3: Matching products");
        
        List<AgentState.ResolvedProduct> resolvedProducts = 
                productMatcherService.matchProducts(state.getIntent().getProducts());
        state.setResolvedProducts(resolvedProducts);
        
        logger.info("Matched {} products", resolvedProducts.size());
        
        return state;
    }
    
    /**
     * Node 4: Create the order with inventory validation.
     * Handles insufficient inventory errors with natural language feedback.
     */
    private AgentState createOrderNode(AgentState state) {
        logger.info("Node 4: Creating order");

        try {
            Order order = orderCreatorService.createOrder(
                    state.getCustomer(),
                    state.getResolvedProducts());
            state.setOrder(order);
            state.setSuccess(true);

            logger.info("Created order: {} (total: ${})", order.getId(), order.getTotal());

        } catch (InsufficientInventoryException e) {
            // Handle insufficient inventory with natural language feedback
            logger.warn("Insufficient inventory for order: {}", e.getMessage());

            state.setSuccess(false);
            state.setErrorMessage(buildInventoryErrorMessage(e));
            state.setResponse(buildInventoryErrorMessage(e));

            logger.info("Order creation failed due to insufficient inventory");
        }

        return state;
    }

    /**
     * Build a natural language error message for insufficient inventory.
     */
    private String buildInventoryErrorMessage(InsufficientInventoryException e) {
        Map<String, InventoryInfo> insufficientProducts = e.getInsufficientProducts();

        if (insufficientProducts.size() == 1) {
            // Single product insufficient
            InventoryInfo info = insufficientProducts.values().iterator().next();
            return String.format(
                    "I'm sorry, but we only have %d '%s' in stock, but you requested %d. " +
                    "Would you like to order %d instead?",
                    info.getAvailable(),
                    info.getProductName(),
                    info.getRequested(),
                    info.getAvailable()
            );
        } else {
            // Multiple products insufficient
            StringBuilder message = new StringBuilder("I'm sorry, but we don't have enough inventory for some products:\n\n");

            for (InventoryInfo info : insufficientProducts.values()) {
                message.append(String.format(
                        "- %s: You requested %d, but we only have %d available\n",
                        info.getProductName(),
                        info.getRequested(),
                        info.getAvailable()
                ));
            }

            message.append("\nWould you like to adjust your order?");
            return message.toString();
        }
    }
    
    /**
     * Node 5: Generate natural language response.
     */
    private AgentState generateResponseNode(AgentState state) {
        logger.info("Node 5: Generating response");
        
        String response = responseGeneratorService.generateSuccessResponse(
                state.getOrder(), 
                state);
        state.setResponse(response);
        
        logger.info("Generated response: {}", response);
        
        return state;
    }
    
    /**
     * Build success response for API.
     */
    private AgentResponse buildSuccessResponse(AgentState state) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setMessage(state.getResponse());
        response.setOrderId(state.getOrder().getId());
        response.setCustomerId(state.getCustomer().getId());
        response.setCustomerName(state.getCustomer().getName());
        response.setTotal(state.getOrder().getTotal().toString());
        response.setItemCount(state.getResolvedProducts().size());
        
        return response;
    }
    
    /**
     * Build error response for API.
     */
    private AgentResponse buildErrorResponse(AgentState state, Exception e) {
        String errorMessage = responseGeneratorService.generateErrorResponse(
                e.getMessage(), 
                state.getUserInput());
        
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setError(e.getMessage());
        
        return response;
    }
}

