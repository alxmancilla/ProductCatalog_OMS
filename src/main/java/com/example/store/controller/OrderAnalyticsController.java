package com.example.store.controller;

import com.example.store.dto.DailyRevenueDTO;
import com.example.store.dto.PopularProductDTO;
import com.example.store.dto.RevenueByStatusDTO;
import com.example.store.dto.TopCustomerDTO;
import com.example.store.service.OrderAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Order Analytics using MongoDB Aggregation Framework.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Demonstrate MongoDB's Aggregation Pipeline Capabilities
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This controller provides analytics endpoints that showcase MongoDB's powerful
 * aggregation framework for:
 * - Revenue analysis by order status
 * - Customer segmentation (top customers)
 * - Product performance (best-selling products)
 * - Time-series analysis (daily revenue trends)
 * 
 * MongoDB Aggregation Benefits:
 * ✅ Server-side processing (not application-side)
 * ✅ Pipeline approach (composable stages)
 * ✅ Supports complex transformations ($unwind, $lookup, $group)
 * ✅ Can use indexes for performance
 * ✅ Results can be materialized to collections
 * 
 * Perfect for webinar demonstrations:
 * - Shows MongoDB's analytical capabilities
 * - Compares to SQL GROUP BY, JOIN
 * - Real-world business intelligence use cases
 * 
 * MongoDB Design Pattern: Computed Pattern
 * - Pre-aggregate data during writes (order.total)
 * - Post-aggregate data during reads (these analytics)
 * - Choose based on read/write ratio
 */
@RestController
@RequestMapping("/analytics/orders")
public class OrderAnalyticsController {

    @Autowired
    private OrderAnalyticsService analyticsService;

    /**
     * Get revenue breakdown by order status.
     * 
     * GET /analytics/orders/revenue-by-status
     * 
     * Example response:
     * [
     *   {
     *     "status": "DELIVERED",
     *     "totalRevenue": 125000.50,
     *     "orderCount": 450,
     *     "averageOrderValue": 277.78
     *   },
     *   {
     *     "status": "CONFIRMED",
     *     "totalRevenue": 85000.25,
     *     "orderCount": 320,
     *     "averageOrderValue": 265.63
     *   }
     * ]
     * 
     * MongoDB Aggregation:
     * db.orders.aggregate([
     *   { $match: { status: { $ne: "CANCELLED" } } },
     *   { $group: { 
     *       _id: "$status",
     *       totalRevenue: { $sum: "$total" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { totalRevenue: -1 } }
     * ])
     * 
     * Use Cases:
     * - Executive dashboards
     * - Revenue reporting
     * - Order pipeline analysis
     * 
     * @return List of revenue statistics grouped by order status
     */
    @GetMapping("/revenue-by-status")
    public ResponseEntity<List<RevenueByStatusDTO>> getRevenueByStatus() {
        List<RevenueByStatusDTO> revenue = analyticsService.getRevenueByStatus();
        return ResponseEntity.ok(revenue);
    }

    /**
     * Get top N customers by total spending.
     * 
     * GET /analytics/orders/top-customers?limit=10
     * 
     * Example response:
     * [
     *   {
     *     "customerId": "cust123",
     *     "customerName": "John Doe",
     *     "totalSpent": 15000.00,
     *     "orderCount": 25,
     *     "averageOrderValue": 600.00
     *   }
     * ]
     * 
     * MongoDB Aggregation:
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
     * 
     * Use Cases:
     * - VIP customer identification
     * - Loyalty program targeting
     * - Customer segmentation
     * 
     * @param limit Maximum number of customers to return (default: 10)
     * @return List of top customers by spending
     */
    @GetMapping("/top-customers")
    public ResponseEntity<List<TopCustomerDTO>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopCustomerDTO> customers = analyticsService.getTopCustomers(limit);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get most popular products by quantity sold.
     * 
     * GET /analytics/orders/popular-products?limit=10
     * 
     * Example response:
     * [
     *   {
     *     "productId": "prod456",
     *     "productName": "Laptop Pro 15",
     *     "totalQuantitySold": 1250,
     *     "totalRevenue": 1624875.00,
     *     "orderCount": 890,
     *     "averagePrice": 1299.90
     *   }
     * ]
     * 
     * MongoDB Aggregation (demonstrates $unwind for embedded arrays):
     * db.orders.aggregate([
     *   { $unwind: "$items" },
     *   { $addFields: { itemRevenue: { $multiply: ["$items.price", "$items.quantity"] } } },
     *   { $group: {
     *       _id: "$items.productId",
     *       productName: { $first: "$items.name" },
     *       totalQuantitySold: { $sum: "$items.quantity" },
     *       totalRevenue: { $sum: "$itemRevenue" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { totalQuantitySold: -1 } },
     *   { $limit: 10 }
     * ])
     *
     * Demonstrates:
     * - $unwind: Deconstruct embedded order items
     * - $addFields: Calculate derived values
     * - Working with embedded documents (Embedding Pattern)
     *
     * Use Cases:
     * - Inventory planning
     * - Marketing campaigns
     * - Product recommendations
     *
     * @param limit Maximum number of products to return (default: 10)
     * @return List of popular products by quantity sold
     */
    @GetMapping("/popular-products")
    public ResponseEntity<List<PopularProductDTO>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<PopularProductDTO> products = analyticsService.getPopularProducts(limit);
        return ResponseEntity.ok(products);
    }

    /**
     * Get daily revenue trends for the last N days.
     *
     * GET /analytics/orders/daily-revenue?days=30
     *
     * Example response:
     * [
     *   {
     *     "date": "2024-03-15",
     *     "totalRevenue": 12500.00,
     *     "orderCount": 45,
     *     "averageOrderValue": 277.78
     *   },
     *   {
     *     "date": "2024-03-14",
     *     "totalRevenue": 11800.00,
     *     "orderCount": 42,
     *     "averageOrderValue": 280.95
     *   }
     * ]
     *
     * MongoDB Aggregation (demonstrates date operations):
     * db.orders.aggregate([
     *   { $match: { orderDate: { $gte: ISODate("2024-02-15") } } },
     *   { $addFields: {
     *       dateString: { $dateToString: { format: "%Y-%m-%d", date: "$orderDate" } }
     *   }},
     *   { $group: {
     *       _id: "$dateString",
     *       totalRevenue: { $sum: "$total" },
     *       orderCount: { $sum: 1 }
     *   }},
     *   { $sort: { _id: -1 } }
     * ])
     *
     * Use Cases:
     * - Revenue trend analysis
     * - Forecasting
     * - Seasonal pattern detection
     * - Dashboard time-series charts
     *
     * @param days Number of days to look back (default: 30)
     * @return List of daily revenue statistics
     */
    @GetMapping("/daily-revenue")
    public ResponseEntity<List<DailyRevenueDTO>> getDailyRevenueTrends(
            @RequestParam(defaultValue = "30") int days) {
        List<DailyRevenueDTO> trends = analyticsService.getDailyRevenueTrends(days);
        return ResponseEntity.ok(trends);
    }
}
