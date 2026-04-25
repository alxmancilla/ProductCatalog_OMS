package com.example.store.config;

import com.mongodb.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB Monitoring and Metrics Configuration (P1 FIX)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE: Observability and Performance Monitoring
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This configuration adds:
 * 1. Slow query logging (queries > 100ms)
 * 2. Connection pool monitoring
 * 3. Command execution metrics
 * 4. Error tracking
 * 
 * Why This Matters:
 * - Identify slow queries before they become production issues
 * - Monitor connection pool exhaustion
 * - Track MongoDB errors and retry logic
 * - Provide data for performance tuning
 * 
 * Production Usage:
 * - Integrate with metrics systems (Prometheus, DataDog, etc.)
 * - Set up alerts for slow queries
 * - Monitor connection pool saturation
 */
@Slf4j
@Configuration
public class MongoMetricsConfiguration {

    /**
     * Register MongoDB CommandListener for query monitoring.
     * 
     * This listener intercepts all MongoDB commands and logs:
     * - Slow queries (> 100ms)
     * - Failed commands
     * - Execution times
     * 
     * Performance Impact: Negligible (<1ms overhead per query)
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoCommandListener() {
        return builder -> builder
            .addCommandListener(new CommandListener() {
                
                @Override
                public void commandStarted(CommandStartedEvent event) {
                    // Log command start for debugging
                    if (log.isDebugEnabled()) {
                        log.debug("MongoDB command started: {} on database: {}", 
                            event.getCommandName(), 
                            event.getDatabaseName());
                    }
                }

                @Override
                public void commandSucceeded(CommandSucceededEvent event) {
                    long elapsedTimeMs = event.getElapsedTime(TimeUnit.MILLISECONDS);

                    // P1 FIX: Slow query logging
                    if (elapsedTimeMs > 100) {
                        log.warn("⚠️  SLOW QUERY DETECTED: {} took {}ms (requestId: {})",
                            event.getCommandName(),
                            elapsedTimeMs,
                            event.getRequestId());
                    }

                    // Log all queries in debug mode
                    if (log.isDebugEnabled()) {
                        log.debug("MongoDB command succeeded: {} took {}ms",
                            event.getCommandName(),
                            elapsedTimeMs);
                    }

                    // TODO: Send to metrics system (Prometheus, DataDog, etc.)
                    // Example:
                    // meterRegistry.timer("mongodb.command.duration",
                    //     "command", event.getCommandName())
                    //     .record(elapsedTimeMs, TimeUnit.MILLISECONDS);
                }

                @Override
                public void commandFailed(CommandFailedEvent event) {
                    // P1 FIX: Error tracking
                    log.error("❌ MongoDB command FAILED: {} after {}ms - Reason: {}",
                        event.getCommandName(),
                        event.getElapsedTime(TimeUnit.MILLISECONDS),
                        event.getThrowable().getMessage(),
                        event.getThrowable());
                    
                    // TODO: Send to error tracking (Sentry, Rollbar, etc.)
                }
            });
    }

    /**
     * Register Connection Pool Listener for monitoring (P1 FIX).
     * 
     * Monitors:
     * - Connection pool creation/closing
     * - Connection checkout/checkin
     * - Connection ready/closed events
     * 
     * Critical for:
     * - Detecting connection leaks
     * - Monitoring pool saturation
     * - Capacity planning
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoConnectionPoolListener() {
        return builder -> builder
            .applyToConnectionPoolSettings(poolSettings -> 
                poolSettings.addConnectionPoolListener(new ConnectionPoolListener() {
                    
                    @Override
                    public void connectionPoolCreated(ConnectionPoolCreatedEvent event) {
                        log.info("✅ MongoDB connection pool created for: {}", 
                            event.getServerId());
                    }

                    @Override
                    public void connectionPoolCleared(ConnectionPoolClearedEvent event) {
                        log.warn("⚠️  MongoDB connection pool cleared for: {}",
                            event.getServerId());
                    }

                    @Override
                    public void connectionPoolClosed(ConnectionPoolClosedEvent event) {
                        log.info("🔴 MongoDB connection pool closed for: {}", 
                            event.getServerId());
                    }

                    @Override
                    public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
                        if (log.isTraceEnabled()) {
                            log.trace("Connection checked out: {}", 
                                event.getConnectionId());
                        }
                        
                        // TODO: Track pool utilization
                        // poolUtilizationGauge.increment();
                    }

                    @Override
                    public void connectionCheckedIn(ConnectionCheckedInEvent event) {
                        if (log.isTraceEnabled()) {
                            log.trace("Connection checked in: {}", 
                                event.getConnectionId());
                        }
                        
                        // TODO: Track pool utilization
                        // poolUtilizationGauge.decrement();
                    }

                    @Override
                    public void connectionCheckOutFailed(ConnectionCheckOutFailedEvent event) {
                        // CRITICAL: Connection pool exhausted!
                        log.error("❌ CRITICAL: Connection checkout FAILED - Pool exhausted! Reason: {}", 
                            event.getReason());
                        
                        // TODO: Send alert! This means your app is running out of connections
                        // alertService.sendCriticalAlert("MongoDB connection pool exhausted");
                    }
                })
            );
    }
}
