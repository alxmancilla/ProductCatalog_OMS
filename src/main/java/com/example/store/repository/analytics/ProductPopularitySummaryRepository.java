package com.example.store.repository.analytics;

import com.example.store.model.analytics.ProductPopularitySummary;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ProductPopularitySummary read model (CQRS)
 */
@Repository
public interface ProductPopularitySummaryRepository extends MongoRepository<ProductPopularitySummary, String> {

    /**
     * Find top N products by total quantity sold
     */
    List<ProductPopularitySummary> findTopByOrderByTotalQuantitySoldDesc(Sort sort);

    /**
     * Find top N products by total revenue
     */
    List<ProductPopularitySummary> findTopByOrderByTotalRevenueDesc(Sort sort);

    /**
     * Find products by category sorted by popularity
     */
    List<ProductPopularitySummary> findByCategoryOrderByTotalQuantitySoldDesc(String category);
}
