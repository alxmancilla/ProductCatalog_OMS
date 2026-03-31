package com.example.store.service;

import com.example.store.dto.DailyRevenueDTO;
import com.example.store.dto.PopularProductDTO;
import com.example.store.dto.RevenueByStatusDTO;
import com.example.store.dto.TopCustomerDTO;
import com.example.store.model.OrderStatus;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for order analytics using MongoDB Aggregation Framework.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Demonstrate MongoDB's Aggregation Pipeline Capabilities
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This service showcases MongoDB's powerful aggregation framework:
 * - $group: Group documents and calculate aggregates
 * - $match: Filter documents (like SQL WHERE)
 * - $project: Shape output documents
 * - $sort: Order results
 * - $limit: Limit result count
 * - $unwind: Deconstruct arrays (for embedded items)
 * - $lookup: Join collections (like SQL JOIN)
 * 
 * MongoDB Aggregation vs SQL:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ SQL                    │ MongoDB Aggregation                │
 * ├────────────────────────┼────────────────────────────────────┤
 * │ SELECT                 │ $project                           │
 * │ FROM                   │ Collection name                    │
 * │ WHERE                  │ $match                             │
 * │ GROUP BY               │ $group                             │
 * │ HAVING                 │ $match (after $group)              │
 * │ ORDER BY               │ $sort                              │
 * │ LIMIT                  │ $limit                             │
 * │ JOIN                   │ $lookup                            │
 * │ UNION                  │ $unionWith                         │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Performance Notes:
 * - Aggregations run on MongoDB server (not application)
 * - Use indexes for $match and $sort stages
 * - Results can be materialized to collections with $merge or $out
 */
@Service
public class OrderAnalyticsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Get revenue breakdown by order status.
     * 
     * MongoDB Aggregation Pipeline:
     * [
     *   { $match: { status: { $ne: "CANCELLED" } } },
     *   { $group: { 
     *       _id: "$status",
     *       totalRevenue: { $sum: "$total" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { totalRevenue: -1 } }
     * ]
     * 
     * SQL Equivalent:
     * SELECT status, 
     *        SUM(total) as totalRevenue, 
     *        COUNT(*) as orderCount,
     *        AVG(total) as averageOrderValue
     * FROM orders
     * WHERE status != 'CANCELLED'
     * GROUP BY status
     * ORDER BY totalRevenue DESC
     */
    public List<RevenueByStatusDTO> getRevenueByStatus() {
        Aggregation aggregation = Aggregation.newAggregation(
            // Stage 1: Filter out cancelled orders (optional)
            Aggregation.match(org.springframework.data.mongodb.core.query.Criteria
                .where("status").ne(OrderStatus.CANCELLED)),
            
            // Stage 2: Group by status and calculate metrics
            Aggregation.group("status")
                .sum("total").as("totalRevenue")
                .count().as("orderCount"),
            
            // Stage 3: Sort by revenue descending
            Aggregation.sort(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "totalRevenue"))
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, "orders", Document.class
        );

        return results.getMappedResults().stream()
            .map(doc -> {
                String statusStr = doc.getString("_id");
                OrderStatus status = OrderStatus.valueOf(statusStr);
                BigDecimal totalRevenue = new BigDecimal(doc.get("totalRevenue").toString());
                Long orderCount = ((Number) doc.get("orderCount")).longValue();
                BigDecimal avgOrderValue = orderCount > 0 
                    ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                
                return new RevenueByStatusDTO(status, totalRevenue, orderCount, avgOrderValue);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get top N customers by total spending.
     * 
     * MongoDB Aggregation Pipeline:
     * [
     *   { $group: { 
     *       _id: "$customerId",
     *       customerName: { $first: "$customerName" },
     *       totalSpent: { $sum: "$total" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { totalSpent: -1 } },
     *   { $limit: limit }
     * ]
     * 
     * Demonstrates:
     * - $first: Get first value from grouped documents
     * - $limit: Limit results (top N)
     */
    public List<TopCustomerDTO> getTopCustomers(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            // Stage 1: Group by customer
            Aggregation.group("customerId")
                .first("customerName").as("customerName")
                .sum("total").as("totalSpent")
                .count().as("orderCount"),
            
            // Stage 2: Sort by total spent
            Aggregation.sort(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "totalSpent")),
            
            // Stage 3: Limit to top N
            Aggregation.limit(limit)
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, "orders", Document.class
        );

        return results.getMappedResults().stream()
            .map(doc -> {
                String customerId = doc.getString("_id");
                String customerName = doc.getString("customerName");
                BigDecimal totalSpent = new BigDecimal(doc.get("totalSpent").toString());
                Long orderCount = ((Number) doc.get("orderCount")).longValue();
                BigDecimal avgOrderValue = orderCount > 0
                    ? totalSpent.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new TopCustomerDTO(customerId, customerName, totalSpent, orderCount, avgOrderValue);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get most popular products by quantity sold.
     *
     * MongoDB Aggregation Pipeline:
     * [
     *   { $unwind: "$items" },
     *   { $group: {
     *       _id: "$items.productId",
     *       productName: { $first: "$items.name" },
     *       totalQuantitySold: { $sum: "$items.quantity" },
     *       totalRevenue: { $sum: { $multiply: ["$items.price", "$items.quantity"] } },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { totalQuantitySold: -1 } },
     *   { $limit: limit }
     * ]
     *
     * Demonstrates:
     * - $unwind: Deconstruct embedded array (items) into separate documents
     * - $multiply: Arithmetic operations in aggregation
     * - Working with embedded documents
     */
    public List<PopularProductDTO> getPopularProducts(int limit) {
        // MongoDB aggregation to find popular products from embedded order items
        // This requires unwinding the items array first

        // Use native MongoDB aggregation for complex calculations
        List<Document> pipeline = List.of(
            // Stage 1: Unwind items array
            new Document("$unwind", "$items"),

            // Stage 2: Add calculated field for item revenue
            new Document("$addFields", new Document()
                .append("itemRevenue", new Document("$multiply", List.of("$items.price", "$items.quantity")))
            ),

            // Stage 3: Group by product
            new Document("$group", new Document()
                .append("_id", "$items.productId")
                .append("productName", new Document("$first", "$items.name"))
                .append("totalQuantitySold", new Document("$sum", "$items.quantity"))
                .append("totalRevenue", new Document("$sum", "$itemRevenue"))
                .append("orderCount", new Document("$sum", 1))
            ),

            // Stage 4: Sort by quantity sold
            new Document("$sort", new Document("totalQuantitySold", -1)),

            // Stage 5: Limit to top N
            new Document("$limit", limit)
        );

        // Execute raw aggregation pipeline
        List<Document> results = mongoTemplate.getCollection("orders")
            .aggregate(pipeline)
            .into(new java.util.ArrayList<>());

        return results.stream()
            .map(doc -> {
                String productId = doc.getString("_id");
                String productName = doc.getString("productName");
                Long totalQuantitySold = ((Number) doc.get("totalQuantitySold")).longValue();

                // Handle totalRevenue which might be a nested document from $sum
                Object revenueObj = doc.get("totalRevenue");
                BigDecimal totalRevenue = BigDecimal.ZERO;
                if (revenueObj instanceof Number) {
                    totalRevenue = new BigDecimal(revenueObj.toString());
                }

                Long orderCount = ((Number) doc.get("orderCount")).longValue();
                BigDecimal avgPrice = totalQuantitySold > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalQuantitySold), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new PopularProductDTO(productId, productName, totalQuantitySold,
                    totalRevenue, orderCount, avgPrice);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get daily revenue trends for the last N days.
     *
     * MongoDB Aggregation Pipeline:
     * [
     *   { $match: { orderDate: { $gte: startDate } } },
     *   { $group: {
     *       _id: { $dateToString: { format: "%Y-%m-%d", date: "$orderDate" } },
     *       revenue: { $sum: "$total" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { _id: -1 } }
     * ]
     *
     * Demonstrates:
     * - $dateToString: Date formatting in aggregation
     * - Date range filtering
     * - Time-series data aggregation
     */
    public List<DailyRevenueDTO> getDailyRevenueTrends(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // Use native MongoDB aggregation for date string formatting
        List<Document> pipeline = List.of(
            // Stage 1: Filter to last N days
            new Document("$match", new Document("orderDate",
                new Document("$gte", startDate))),

            // Stage 2: Add date string field
            new Document("$addFields", new Document()
                .append("dateString", new Document("$dateToString",
                    new Document("format", "%Y-%m-%d").append("date", "$orderDate")))
            ),

            // Stage 3: Group by date string
            new Document("$group", new Document()
                .append("_id", "$dateString")
                .append("totalRevenue", new Document("$sum", "$total"))
                .append("orderCount", new Document("$sum", 1))
            ),

            // Stage 4: Sort by date descending (newest first)
            new Document("$sort", new Document("_id", -1))
        );

        // Execute raw aggregation pipeline
        List<Document> results = mongoTemplate.getCollection("orders")
            .aggregate(pipeline)
            .into(new java.util.ArrayList<>());

        return results.stream()
            .map(doc -> {
                String dateString = doc.getString("_id");
                LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
                BigDecimal totalRevenue = new BigDecimal(doc.get("totalRevenue").toString());
                Long orderCount = ((Number) doc.get("orderCount")).longValue();
                BigDecimal avgOrderValue = orderCount > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new DailyRevenueDTO(date, totalRevenue, orderCount, avgOrderValue);
            })
            .collect(Collectors.toList());
    }
}

