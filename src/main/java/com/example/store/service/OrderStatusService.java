package com.example.store.service;

import com.example.store.exception.OrderNotFoundException;
import com.example.store.model.Order;
import com.example.store.model.OrderStatus;
import com.example.store.model.StatusChange;
import com.example.store.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling order status management with validation and audit trail.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Order Status Lifecycle Management
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service ensures that:
 * 1. Status transitions follow business rules
 * 2. Status changes are recorded in history (audit trail)
 * 3. Current status is always denormalized (Computed Pattern)
 * 4. Updates are atomic (MongoDB document updates)
 *
 * Status Flow:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *    ↓
 * CANCELLED (from PENDING or CONFIRMED only)
 *
 * MongoDB Pattern: EMBEDDING + COMPUTED
 * - Current status stored in `status` field (fast queries)
 * - Complete history stored in `statusHistory` array (audit trail)
 * - Single document update = atomic operation!
 */
@Service
public class OrderStatusService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Update order status with validation and history tracking.
     * 
     * @param orderId The order ID to update
     * @param newStatus The new status to set
     * @param changedBy Who is making the change
     * @param reason Optional reason for the change
     * @return The updated order
     * @throws OrderNotFoundException if order not found
     * @throws IllegalStateException if status transition is not allowed
     */
    @Transactional  // Not strictly required for single-document updates, but good practice
    public Order updateOrderStatus(String orderId, OrderStatus newStatus,
                                   String changedBy, String reason) {
        return updateOrderStatus(orderId, newStatus, changedBy, reason, null);
    }

    /**
     * Update order status with validation, history tracking, and metadata.
     *
     * @param orderId The order ID to update
     * @param newStatus The new status to set
     * @param changedBy Who is making the change
     * @param reason Optional reason for the change
     * @param metadata Optional metadata (e.g., tracking number for SHIPPED)
     * @return The updated order
     * @throws OrderNotFoundException if order not found
     * @throws IllegalStateException if status transition is not allowed
     */
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus,
                                   String changedBy, String reason,
                                   Map<String, Object> metadata) {
        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Find the order
        // ═══════════════════════════════════════════════════════════════════
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus currentStatus = order.getStatus();

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Validate status transition
        // ═══════════════════════════════════════════════════════════════════
        validateStatusTransition(currentStatus, newStatus);

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Create status change record (audit trail)
        // ═══════════════════════════════════════════════════════════════════
        StatusChange change = new StatusChange();
        change.setFromStatus(currentStatus);
        change.setToStatus(newStatus);
        change.setChangedAt(LocalDateTime.now());
        change.setChangedBy(changedBy);
        change.setReason(reason);
        
        if (metadata != null && !metadata.isEmpty()) {
            change.setMetadata(metadata);
        } else {
            change.setMetadata(new HashMap<>());
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 4: Update order (ATOMIC operation!)
        // ═══════════════════════════════════════════════════════════════════
        // MongoDB best practice: Single document update is atomic
        // - Update current status (denormalized for fast queries)
        // - Add to status history array (complete audit trail)
        order.setStatus(newStatus);
        order.getStatusHistory().add(change);

        // Save and return
        Order updatedOrder = orderRepository.save(order);
        return updatedOrder;
    }

    /**
     * Validate that a status transition is allowed.
     * 
     * Business Rules:
     * - PENDING → CONFIRMED, CANCELLED ✅
     * - CONFIRMED → PROCESSING, CANCELLED ✅
     * - PROCESSING → SHIPPED ✅
     * - SHIPPED → DELIVERED ✅
     * - DELIVERED → (terminal state) ❌
     * - CANCELLED → (terminal state) ❌
     * - No backwards movement (except CANCELLED) ❌
     * 
     * @param from Current status
     * @param to Desired status
     * @throws IllegalStateException if transition is not allowed
     */
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Same status = no-op
        if (from == to) {
            return;
        }

        // Check if current status is terminal
        if (from == OrderStatus.DELIVERED || from == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                String.format("Cannot change status from %s (terminal state)", from));
        }

        // Define allowed transitions
        boolean isAllowed = false;

        switch (from) {
            case PENDING:
                isAllowed = (to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED);
                break;
            case CONFIRMED:
                isAllowed = (to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED);
                break;
            case PROCESSING:
                isAllowed = (to == OrderStatus.SHIPPED);
                break;
            case SHIPPED:
                isAllowed = (to == OrderStatus.DELIVERED);
                break;
            default:
                isAllowed = false;
        }

        if (!isAllowed) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s → %s", from, to));
        }
    }
}

