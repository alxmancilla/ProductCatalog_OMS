package com.example.store.controller;

import com.example.store.dto.CustomerLifetimeValueDTO;
import com.example.store.service.OrderAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Customer Analytics.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 DEMONSTRATES: $lookup — MongoDB's JOIN equivalent
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Endpoints:
 *   GET /analytics/customers/lifetime-value?limit=10
 *
 * This controller complements OrderAnalyticsController by showing that
 * MongoDB aggregation can JOIN across collections using $lookup — while
 * also demonstrating that the Subset Pattern (customerName stored in orders)
 * often eliminates the need for a join in the first place.
 */
@RestController
@RequestMapping("/analytics/customers")
@RequiredArgsConstructor
public class CustomerAnalyticsController {

    private final OrderAnalyticsService analyticsService;

    /**
     * Get Customer Lifetime Value (CLV) for the top N customers.
     *
     * GET /analytics/customers/lifetime-value?limit=10
     *
     * Example response:
     * [
     *   {
     *     "customerId":     "abc123",
     *     "customerName":   "Alice Johnson",
     *     "email":          "alice@example.com",
     *     "totalSpent":     15240.50,
     *     "orderCount":     12,
     *     "avgOrderValue":  1270.04,
     *     "firstOrderDate": "2024-01-10",
     *     "lastOrderDate":  "2026-03-28"
     *   }
     * ]
     *
     * MongoDB Aggregation (mongosh):
     * db.orders.aggregate([
     *   { $match: { status: { $ne: "CANCELLED" } } },
     *   { $group: {
     *       _id: "$customerId",
     *       customerName:   { $first: "$customerName" },
     *       totalSpent:     { $sum:   "$total" },
     *       orderCount:     { $sum:   1 },
     *       avgOrderValue:  { $avg:   "$total" },
     *       firstOrderDate: { $min:   "$orderDate" },
     *       lastOrderDate:  { $max:   "$orderDate" }
     *   }},
     *   { $lookup: {
     *       from: "customers", localField: "_id",
     *       foreignField: "_id", as: "customerDetails"
     *   }},
     *   { $addFields: { email: { $first: "$customerDetails.email" } } },
     *   { $project: { customerDetails: 0 } },
     *   { $sort: { totalSpent: -1 } },
     *   { $limit: 10 }
     * ])
     *
     * Use cases: loyalty tiers, VIP outreach, churn prediction, CLV dashboards.
     *
     * @param limit Maximum customers to return (default: 10)
     */
    @GetMapping("/lifetime-value")
    public ResponseEntity<List<CustomerLifetimeValueDTO>> getCustomerLifetimeValue(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getCustomerLifetimeValue(limit));
    }
}
