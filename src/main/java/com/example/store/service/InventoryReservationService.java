package com.example.store.service;

import com.example.store.model.InventoryReservation;
import com.example.store.model.Product;
import com.example.store.repository.InventoryReservationRepository;
import com.example.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing inventory reservations.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: MongoDB TTL Index — auto-expiring documents
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Available inventory = product.inventory
 *                     - SUM(active reservation quantities for that product)
 *
 * MongoDB handles cleanup automatically: when expiresAt passes, the TTL
 * monitor removes the document, freeing the hold with zero application code.
 */
@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    /** Default hold duration when caller does not specify. */
    private static final int DEFAULT_HOLD_MINUTES = 15;

    // ─────────────────────────────────────────────────────────────────────
    // Reserve
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Place a hold on product inventory for up to holdMinutes minutes.
     *
     * @return a Map with the saved reservation plus availability snapshot
     * @throws IllegalArgumentException if the product is not found or
     *                                  there is not enough available inventory
     */
    public Map<String, Object> reserve(String productId, int quantity,
                                       String customerId, String customerName,
                                       int holdMinutes) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Product not found: " + productId));

        int reserved   = getReservedQuantity(productId);
        int available  = product.getInventory() - reserved;

        if (quantity > available) {
            throw new IllegalArgumentException(
                "Not enough available inventory for '" + product.getName() +
                "'. Available: " + available + ", requested: " + quantity);
        }

        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(holdMinutes > 0 ? holdMinutes : DEFAULT_HOLD_MINUTES);

        InventoryReservation reservation = new InventoryReservation(
            null, productId, product.getName(), quantity,
            customerId, customerName, now, expiresAt
        );
        reservation = reservationRepository.save(reservation);

        int newReserved   = reserved + quantity;
        int newAvailable  = product.getInventory() - newReserved;

        Map<String, Object> result = new HashMap<>();
        result.put("reservationId",      reservation.getId());
        result.put("productId",          productId);
        result.put("productName",        product.getName());
        result.put("quantity",           quantity);
        result.put("inventoryTotal",     product.getInventory());
        result.put("inventoryReserved",  newReserved);
        result.put("inventoryAvailable", newAvailable);
        result.put("reservedAt",         now.toString());
        result.put("expiresAt",          expiresAt.toString());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Release
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Explicitly release a reservation before it expires (e.g., customer
     * cancels checkout). MongoDB would delete it automatically via TTL anyway,
     * but explicit release frees the inventory immediately.
     */
    public void release(String reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new IllegalArgumentException(
                "Reservation not found (may have already expired): " + reservationId);
        }
        reservationRepository.deleteById(reservationId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Availability
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Return current availability snapshot for a product.
     */
    public Map<String, Object> getAvailability(String productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Product not found: " + productId));

        List<InventoryReservation> active = reservationRepository.findByProductId(productId);
        int reserved  = active.stream().mapToInt(InventoryReservation::getQuantity).sum();
        int available = product.getInventory() - reserved;

        Map<String, Object> result = new HashMap<>();
        result.put("productId",          productId);
        result.put("productName",        product.getName());
        result.put("inventoryTotal",     product.getInventory());
        result.put("inventoryReserved",  reserved);
        result.put("inventoryAvailable", available);
        result.put("activeReservations", active.size());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private int getReservedQuantity(String productId) {
        return reservationRepository.findByProductId(productId)
            .stream()
            .mapToInt(InventoryReservation::getQuantity)
            .sum();
    }
}
