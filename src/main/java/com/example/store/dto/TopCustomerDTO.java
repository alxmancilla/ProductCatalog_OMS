package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for top customers by revenue/order count.
 * 
 * Used by analytics endpoints to show best customers.
 * 
 * Example aggregation:
 * db.orders.aggregate([
 *   { $group: { 
 *       _id: "$customerId", 
 *       customerName: { $first: "$customerName" },
 *       totalSpent: { $sum: "$total" },
 *       orderCount: { $sum: 1 }
 *   }},
 *   { $sort: { totalSpent: -1 } },
 *   { $limit: 10 }
 * ])
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerDTO {
    private String customerId;
    private String customerName;
    private BigDecimal totalSpent;
    private Long orderCount;
    private BigDecimal averageOrderValue;
}

