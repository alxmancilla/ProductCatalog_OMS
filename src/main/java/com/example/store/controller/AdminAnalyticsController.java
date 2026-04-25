package com.example.store.controller;

import com.example.store.service.analytics.MaterializedViewRebuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller for Managing CQRS Materialized Views
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Administrative Operations for Read Models
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This controller provides admin endpoints to:
 * - Rebuild materialized views from source data
 * - Check view statistics
 * - Verify data consistency
 * 
 * Security Note:
 * In production, these endpoints should be protected with admin authentication.
 * Consider using Spring Security with role-based access control.
 * 
 * URL Pattern: /admin/analytics/*
 * 
 * Common Use Cases:
 * - Initial deployment: rebuild views from existing data
 * - After bug fix: rebuild to fix corrupted data
 * - Testing: verify view counts match expected values
 */
@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final MaterializedViewRebuildService rebuildService;

    /**
     * Rebuild all materialized views from orders collection.
     * 
     * POST /admin/analytics/rebuild-all
     * 
     * WARNING: This clears and rebuilds ALL views!
     * Use during off-peak hours for large datasets.
     * 
     * @return Number of orders processed
     */
    @PostMapping("/rebuild-all")
    public ResponseEntity<Map<String, Object>> rebuildAllViews() {
        long count = rebuildService.rebuildAllViews();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("ordersProcessed", count);
        response.put("message", "All materialized views rebuilt successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rebuild only daily revenue summaries.
     * 
     * POST /admin/analytics/rebuild-daily-revenue
     * 
     * @return Number of orders processed
     */
    @PostMapping("/rebuild-daily-revenue")
    public ResponseEntity<Map<String, Object>> rebuildDailyRevenue() {
        long count = rebuildService.rebuildDailyRevenueSummaries();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("ordersProcessed", count);
        response.put("message", "Daily revenue summaries rebuilt");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rebuild only product popularity summaries.
     * 
     * POST /admin/analytics/rebuild-product-popularity
     * 
     * @return Number of orders processed
     */
    @PostMapping("/rebuild-product-popularity")
    public ResponseEntity<Map<String, Object>> rebuildProductPopularity() {
        long count = rebuildService.rebuildProductPopularitySummaries();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("ordersProcessed", count);
        response.put("message", "Product popularity summaries rebuilt");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rebuild only customer spending summaries.
     * 
     * POST /admin/analytics/rebuild-customer-spending
     * 
     * @return Number of orders processed
     */
    @PostMapping("/rebuild-customer-spending")
    public ResponseEntity<Map<String, Object>> rebuildCustomerSpending() {
        long count = rebuildService.rebuildCustomerSpendingSummaries();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("ordersProcessed", count);
        response.put("message", "Customer spending summaries rebuilt");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get statistics about materialized views.
     * 
     * GET /admin/analytics/view-stats
     * 
     * Useful for verifying views are properly populated.
     * 
     * @return View statistics
     */
    @GetMapping("/view-stats")
    public ResponseEntity<MaterializedViewRebuildService.ViewStatistics> getViewStats() {
        MaterializedViewRebuildService.ViewStatistics stats = rebuildService.getViewStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check for materialized views.
     * 
     * GET /admin/analytics/health
     * 
     * Returns status based on view completeness.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        MaterializedViewRebuildService.ViewStatistics stats = rebuildService.getViewStatistics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalOrders", stats.getTotalOrders());
        response.put("views", Map.of(
            "dailyRevenue", stats.getDailyRevenueSummaries(),
            "productPopularity", stats.getProductPopularitySummaries(),
            "customerSpending", stats.getCustomerSpendingSummaries()
        ));
        
        // Simple health check: views should have data if orders exist
        boolean healthy = stats.getTotalOrders() == 0 || 
                         (stats.getDailyRevenueSummaries() > 0 && 
                          stats.getProductPopularitySummaries() > 0 && 
                          stats.getCustomerSpendingSummaries() > 0);
        
        response.put("status", healthy ? "UP" : "DEGRADED");
        response.put("message", healthy ? 
            "Materialized views are healthy" : 
            "Materialized views may need rebuild");
        
        return ResponseEntity.ok(response);
    }
}
