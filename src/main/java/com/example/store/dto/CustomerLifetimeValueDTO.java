package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Customer Lifetime Value (CLV) analytics.
 *
 * Demonstrates the $lookup stage — joining the orders collection with the
 * customers collection to enrich grouped results with customer email.
 *
 * MongoDB Aggregation Pipeline used:
 *
 * db.orders.aggregate([
 *   { $match: { status: { $ne: "CANCELLED" } } },
 *   { $group: {
 *       _id: "$customerId",
 *       customerName: { $first: "$customerName" },
 *       totalSpent:   { $sum:   "$total" },
 *       orderCount:   { $sum:   1 },
 *       avgOrderValue:{ $avg:   "$total" },
 *       firstOrderDate: { $min: "$orderDate" },
 *       lastOrderDate:  { $max: "$orderDate" }
 *   }},
 *   { $lookup: {
 *       from: "customers",
 *       localField: "_id",       // customerId (after $group)
 *       foreignField: "_id",     // customer document _id
 *       as: "customerDetails"
 *   }},
 *   { $addFields: { email: { $first: "$customerDetails.email" } } },
 *   { $project: { customerDetails: 0 } },
 *   { $sort: { totalSpent: -1 } },
 *   { $limit: 10 }
 * ])
 *
 * Key teaching points:
 * - $lookup is MongoDB's JOIN equivalent — works across collections
 * - The Subset Pattern (customerName in orders) means we can compute CLV
 *   without $lookup at all — the join is only needed to get email
 * - $first/$min/$max accumulate values across grouped documents
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLifetimeValueDTO {
    private String customerId;
    private String customerName;
    private String email;             // enriched via $lookup from customers
    private BigDecimal totalSpent;
    private Long orderCount;
    private BigDecimal avgOrderValue;
    private String firstOrderDate;    // ISO date string (YYYY-MM-DD)
    private String lastOrderDate;     // ISO date string (YYYY-MM-DD)
}
