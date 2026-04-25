package com.example.store.controller;

import com.example.store.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Inventory Reservations.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: MongoDB TTL (Time-To-Live) Index
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Endpoints:
 *   POST   /inventory/reserve                    — place a hold
 *   DELETE /inventory/reservations/{id}          — release early
 *   GET    /inventory/{productId}/availability   — check available stock
 *
 * The TTL index on inventory_reservations.expiresAt automatically deletes
 * expired documents — no scheduled jobs or cleanup endpoints required.
 *
 * Example flow:
 *   1. POST  /inventory/reserve                  → reservationId = "res_abc"
 *   2. GET   /inventory/prod_456/availability    → inventoryAvailable = 48
 *   3. ... customer completes checkout → POST /orders (uses normal flow)
 *   4. DELETE /inventory/reservations/res_abc    → frees hold immediately
 *      OR: wait 15 min → MongoDB TTL deletes it automatically
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryReservationController {

    private final InventoryReservationService reservationService;

    /**
     * Place a temporary hold on product inventory.
     *
     * POST /inventory/reserve
     *
     * Request body:
     * {
     *   "productId":    "abc123",   ← required
     *   "quantity":     2,          ← required
     *   "customerId":   "cust456",  ← optional
     *   "customerName": "Alice",    ← optional
     *   "holdMinutes":  15          ← optional, default 15
     * }
     *
     * Response (201 Created):
     * {
     *   "reservationId":      "res_789",
     *   "productId":          "abc123",
     *   "productName":        "Laptop Pro 15",
     *   "quantity":           2,
     *   "inventoryTotal":     50,
     *   "inventoryReserved":  2,
     *   "inventoryAvailable": 48,
     *   "reservedAt":         "2026-04-11T10:00:00",
     *   "expiresAt":          "2026-04-11T10:15:00"
     * }
     *
     * Returns 400 if the product is not found or available inventory < quantity.
     */
    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserve(@RequestBody Map<String, Object> body) {
        try {
            String productId    = (String) body.get("productId");
            int    quantity     = ((Number) body.get("quantity")).intValue();
            String customerId   = (String) body.getOrDefault("customerId",   null);
            String customerName = (String) body.getOrDefault("customerName", null);
            int    holdMinutes  = body.containsKey("holdMinutes")
                ? ((Number) body.get("holdMinutes")).intValue() : 15;

            if (productId == null || productId.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "productId is required"));
            }
            if (quantity <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity must be greater than zero"));
            }

            Map<String, Object> result = reservationService.reserve(
                productId, quantity, customerId, customerName, holdMinutes);
            return new ResponseEntity<>(result, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Explicitly release a reservation before it expires.
     *
     * DELETE /inventory/reservations/{id}
     *
     * Returns 204 No Content on success, 404 if not found (already expired or released).
     */
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Map<String, Object>> release(@PathVariable String id) {
        try {
            reservationService.release(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current availability for a product.
     *
     * GET /inventory/{productId}/availability
     *
     * Response:
     * {
     *   "productId":          "abc123",
     *   "productName":        "Laptop Pro 15",
     *   "inventoryTotal":     50,
     *   "inventoryReserved":  4,
     *   "inventoryAvailable": 46,
     *   "activeReservations": 2
     * }
     *
     * inventoryReserved = sum of all active (non-expired) reservation quantities.
     * Expired reservations are automatically removed by the TTL index, so they
     * never appear here.
     */
    @GetMapping("/{productId}/availability")
    public ResponseEntity<Map<String, Object>> getAvailability(@PathVariable String productId) {
        try {
            return ResponseEntity.ok(reservationService.getAvailability(productId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
