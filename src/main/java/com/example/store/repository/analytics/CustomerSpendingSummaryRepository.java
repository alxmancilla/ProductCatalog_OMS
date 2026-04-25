package com.example.store.repository.analytics;

import com.example.store.model.analytics.CustomerSpendingSummary;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CustomerSpendingSummary read model (CQRS)
 */
@Repository
public interface CustomerSpendingSummaryRepository extends MongoRepository<CustomerSpendingSummary, String> {

    /**
     * Find top N customers by total spending
     */
    List<CustomerSpendingSummary> findTopByOrderByTotalSpentDesc(Sort sort);

    /**
     * Find customers by segment
     */
    List<CustomerSpendingSummary> findBySegment(String segment);

    /**
     * Find customers by tier sorted by spending
     */
    List<CustomerSpendingSummary> findByTierOrderByTotalSpentDesc(String tier);
}
