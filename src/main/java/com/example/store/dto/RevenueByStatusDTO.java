package com.example.store.dto;

import com.example.store.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for revenue aggregated by order status.
 * 
 * Used by analytics endpoints to return revenue breakdown.
 * 
 * Example aggregation:
 * db.orders.aggregate([
 *   { $group: { 
 *       _id: "$status", 
 *       revenue: { $sum: "$total" },
 *       orderCount: { $sum: 1 }
 *   }},
 *   { $sort: { revenue: -1 } }
 * ])
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByStatusDTO {
    private OrderStatus status;
    private BigDecimal totalRevenue;
    private Long orderCount;
    private BigDecimal averageOrderValue;
}

