package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * StatusChange - represents a single status transition in an order's lifecycle.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎓 FOR BEGINNERS: This is an EMBEDDED document (stored inside Order)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Notice: This class has NO @Document annotation!
 * Why? Because StatusChange objects are NOT stored in their own collection.
 * They are stored INSIDE the Order document as an array.
 *
 * MongoDB Best Practice: EMBEDDING PATTERN
 * Question: When I view an order, do I need to see its status history?
 * Answer: FREQUENTLY! ✅
 *
 * Solution: Store status history INSIDE the order document
 * Benefit: One query gets order + complete audit trail!
 *
 * ───────────────────────────────────────────────────────────────────────────
 * WHY EMBED STATUS HISTORY INSTEAD OF SEPARATE COLLECTION?
 * ───────────────────────────────────────────────────────────────────────────
 *
 * ❌ Bad Approach (Relational Style):
 * - orders collection: { _id, status, ... }
 * - order_status_history collection: { orderId, fromStatus, toStatus, ... }
 * - Requires JOIN to get order + history
 * - Two queries instead of one
 * - Slower performance
 *
 * ✅ Good Approach (MongoDB Style):
 * - orders collection: { _id, status, statusHistory: [...], ... }
 * - Single query gets everything
 * - Atomic updates (add to statusHistory array)
 * - Fast reads (all data together)
 *
 * ───────────────────────────────────────────────────────────────────────────
 * EXAMPLE: Order with Status History
 * ───────────────────────────────────────────────────────────────────────────
 *
 * {
 *   "_id": "order123",
 *   "status": "SHIPPED",  ← Current status (denormalized for fast reads)
 *   "statusHistory": [    ← Complete audit trail (embedded)
 *     {
 *       "fromStatus": null,
 *       "toStatus": "PENDING",
 *       "changedAt": "2024-01-15T10:00:00",
 *       "changedBy": "system",
 *       "reason": "Order created"
 *     },
 *     {
 *       "fromStatus": "PENDING",
 *       "toStatus": "CONFIRMED",
 *       "changedAt": "2024-01-15T10:05:00",
 *       "changedBy": "admin@example.com",
 *       "reason": "Payment confirmed",
 *       "metadata": {
 *         "paymentId": "pay_12345",
 *         "amount": 1299.99
 *       }
 *     },
 *     {
 *       "fromStatus": "CONFIRMED",
 *       "toStatus": "SHIPPED",
 *       "changedAt": "2024-01-15T14:00:00",
 *       "changedBy": "warehouse-system",
 *       "reason": "Package dispatched",
 *       "metadata": {
 *         "trackingNumber": "1Z999AA10123456784",
 *         "carrier": "UPS"
 *       }
 *     }
 *   ],
 *   ...other order fields...
 * }
 *
 * ───────────────────────────────────────────────────────────────────────────
 */
@Data                                    // Lombok: generates getters/setters
@NoArgsConstructor                       // Lombok: generates no-args constructor
@AllArgsConstructor                      // Lombok: generates all-args constructor
public class StatusChange {

    /**
     * The previous status (null if this is the initial status).
     */
    private OrderStatus fromStatus;

    /**
     * The new status after this change.
     */
    private OrderStatus toStatus;

    /**
     * When this status change occurred.
     */
    private LocalDateTime changedAt;

    /**
     * Who made this status change.
     * Examples: "system", "admin@example.com", "warehouse-system", "customer"
     */
    private String changedBy;

    /**
     * Optional reason for the status change.
     * Examples: "Order created", "Payment confirmed", "Customer requested cancellation"
     */
    private String reason;

    /**
     * Optional metadata for additional context.
     * Flexible field for storing extra information like tracking numbers, payment IDs, etc.
     * 
     * Examples:
     * - For SHIPPED: { "trackingNumber": "1Z999AA10123456784", "carrier": "UPS" }
     * - For CANCELLED: { "refundId": "ref_12345", "refundAmount": 1299.99 }
     * - For CONFIRMED: { "paymentId": "pay_12345", "paymentMethod": "CREDIT_CARD" }
     */
    private Map<String, Object> metadata = new HashMap<>();
}

