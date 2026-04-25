package com.example.store.repository.analytics;

import com.example.store.model.analytics.DailyRevenueSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DailyRevenueSummary read model (CQRS)
 */
@Repository
public interface DailyRevenueSummaryRepository extends MongoRepository<DailyRevenueSummary, String> {

    /**
     * Find summary for specific date and status
     */
    Optional<DailyRevenueSummary> findByDateAndStatus(LocalDate date, String status);

    /**
     * Find all summaries for a specific date (all statuses)
     */
    List<DailyRevenueSummary> findByDate(LocalDate date);

    /**
     * Find summaries in date range (for trend analysis)
     */
    List<DailyRevenueSummary> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find summaries for specific status in date range
     */
    List<DailyRevenueSummary> findByDateBetweenAndStatus(
        LocalDate startDate, 
        LocalDate endDate, 
        String status
    );
}
