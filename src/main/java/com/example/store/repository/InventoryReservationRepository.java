package com.example.store.repository;

import com.example.store.model.InventoryReservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for inventory reservations.
 *
 * Spring Data MongoDB derives all queries from method names — no SQL needed.
 * MongoDB also deletes expired documents automatically via the TTL index on
 * the expiresAt field, so there is no deleteExpired() method here.
 */
@Repository
public interface InventoryReservationRepository extends MongoRepository<InventoryReservation, String> {

    /** All active reservations for a given product (used to compute available inventory). */
    List<InventoryReservation> findByProductId(String productId);
}
