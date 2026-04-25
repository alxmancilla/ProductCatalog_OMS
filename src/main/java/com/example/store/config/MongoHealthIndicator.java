package com.example.store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom MongoDB Health Indicator (P1 FIX)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Health Check for Monitoring Systems
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This health indicator:
 * - Checks MongoDB connectivity
 * - Reports database name
 * - Measures response time
 * - Exposes via /actuator/health endpoint
 * 
 * Usage:
 * - GET /actuator/health
 * - Kubernetes liveness/readiness probes
 * - Load balancer health checks
 * - Monitoring dashboards (DataDog, Prometheus, etc.)
 * 
 * Response Example:
 * {
 *   "status": "UP",
 *   "components": {
 *     "mongo": {
 *       "status": "UP",
 *       "details": {
 *         "database": "product_catalog",
 *         "responseTime": "5ms"
 *       }
 *     }
 *   }
 * }
 */
@Slf4j
@Component("mongo")
@RequiredArgsConstructor
public class MongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    /**
     * Perform health check by pinging MongoDB.
     * 
     * This runs every time /actuator/health is called.
     * 
     * Implementation:
     * - Executes { ping: 1 } command
     * - Measures response time
     * - Returns UP if successful, DOWN if failed
     * 
     * Performance: <5ms typical, <50ms worst case
     */
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Execute MongoDB ping command
            Document result = mongoTemplate.executeCommand("{ ping: 1 }");
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Check if MongoDB responded with ok: 1
            if (result.get("ok", Number.class).intValue() == 1) {
                return Health.up()
                    .withDetail("database", mongoTemplate.getDb().getName())
                    .withDetail("responseTime", responseTime + "ms")
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "MongoDB ping returned non-ok status")
                    .withDetail("response", result.toJson())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            
            return Health.down()
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
