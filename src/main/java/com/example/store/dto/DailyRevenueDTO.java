package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily revenue trends.
 * 
 * Used by analytics endpoints to show revenue over time.
 * 
 * Example aggregation:
 * db.orders.aggregate([
 *   { $group: { 
 *       _id: { $dateToString: { format: "%Y-%m-%d", date: "$orderDate" } },
 *       revenue: { $sum: "$total" },
 *       orderCount: { $sum: 1 }
 *   }},
 *   { $sort: { _id: -1 } }
 * ])
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueDTO {
    private LocalDate date;
    private BigDecimal totalRevenue;
    private Long orderCount;
    private BigDecimal averageOrderValue;
}

