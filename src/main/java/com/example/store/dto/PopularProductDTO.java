package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for popular products by quantity sold.
 * 
 * Used by analytics endpoints to show best-selling products.
 * 
 * Example aggregation:
 * db.orders.aggregate([
 *   { $unwind: "$items" },
 *   { $group: { 
 *       _id: "$items.productId",
 *       productName: { $first: "$items.name" },
 *       totalQuantitySold: { $sum: "$items.quantity" },
 *       totalRevenue: { $sum: { $multiply: ["$items.price", "$items.quantity"] } },
 *       orderCount: { $sum: 1 }
 *   }},
 *   { $sort: { totalQuantitySold: -1 } }
 * ])
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductDTO {
    private String productId;
    private String productName;
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
    private Long orderCount;
    private BigDecimal averagePrice;
}

