package com.example.store.config;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.bson.types.Decimal128;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB Configuration for proper BigDecimal handling and ACID transactions.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE 1: Store BigDecimal as Decimal128 (not String)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * By default, Spring Data MongoDB stores BigDecimal as a String to preserve
 * precision. However, this prevents numeric operations in MongoDB queries.
 *
 * This configuration converts BigDecimal to MongoDB's native Decimal128 type,
 * which:
 * - Stores numbers as actual numbers (not strings)
 * - Preserves precision (128-bit decimal)
 * - Enables numeric queries and aggregations
 * - Shows correctly in MongoDB Compass and shell
 *
 * Example - Product:
 * WITHOUT this config:
 * {
 *   "price": "1299.99"  ← Stored as string
 * }
 *
 * WITH this config:
 * {
 *   "price": NumberDecimal("1299.99")  ← Stored as Decimal128
 * }
 *
 * Example - Order with embedded OrderItems:
 * WITHOUT this config:
 * {
 *   "total": "1359.97",
 *   "items": [
 *     { "name": "Laptop", "price": "1299.99", "quantity": 1 }  ← String
 *   ]
 * }
 *
 * WITH this config:
 * {
 *   "total": NumberDecimal("1359.97"),
 *   "items": [
 *     { "name": "Laptop", "price": NumberDecimal("1299.99"), "quantity": 1 }  ← Decimal128
 *   ]
 * }
 *
 * This converter applies to ALL BigDecimal fields in:
 * - Product.price
 * - OrderItem.price (embedded in Order)
 * - Order.total
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE 2: Enable MongoDB ACID Transactions
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * MongoDB transactions require a replica set configuration. This enables:
 * - Atomic operations across multiple documents
 * - Automatic rollback on failure
 * - Inventory management with order creation
 *
 * IMPORTANT: MongoDB must be running as a replica set for transactions to work!
 * See docker-compose.yml for replica set configuration.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * 🎯 PURPOSE 3: Production-Ready Write Concerns (P0 FIX)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Write Concern: MAJORITY + JOURNALED
 * - w=majority: Waits for write to be acknowledged by majority of replica set
 * - journal=true: Waits for write to be written to the on-disk journal
 * - Prevents data loss if primary fails before replication completes
 * - Critical for financial transactions (orders, payments, inventory)
 *
 * Read Preference: PRIMARY_PREFERRED
 * - Reads from primary by default (strong consistency)
 * - Falls back to secondary if primary unavailable
 *
 * Read Concern: MAJORITY
 * - Only reads data acknowledged by majority of replica set
 * - Prevents reading data that might be rolled back
 *
 * Trade-offs:
 * - Higher latency (~5-50ms) due to replication wait
 * - Better durability (data survives primary failure)
 * - Production best practice for critical data
 */
@Configuration
@EnableTransactionManagement
public class MongoConfig {

    /**
     * Register custom converters for BigDecimal ↔ Decimal128.
     */
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new BigDecimalToDecimal128Converter(),
            new Decimal128ToBigDecimalConverter()
        ));
    }

    /**
     * Transaction manager for MongoDB ACID transactions.
     * Requires MongoDB to be running as a replica set.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    /**
     * Configure production-ready MongoDB client settings (P0 & P1 FIXES)
     *
     * This configures:
     * - Write concern, read preference, read concern (P0)
     * - Connection pool settings (P1)
     * - Timeouts (P1)
     *
     * Write Concern: MAJORITY with journal=true, 5s timeout (P0)
     * - Prevents data loss on primary failure
     * - Ensures data is durable before returning
     *
     * Read Preference: PRIMARY_PREFERRED (P0)
     * - Strong consistency (read from primary)
     * - Failover capability (read from secondary if needed)
     *
     * Read Concern: MAJORITY (P0)
     * - Only read majority-committed data
     * - Prevents dirty reads
     *
     * Connection Pool (P1 FIX):
     * - maxPoolSize: 50 (tune based on traffic - see below)
     * - minPoolSize: 10 (pre-warm connections)
     * - maxIdleTime: 60s (cleanup idle connections)
     * - maxConnecting: 2 (prevent connection storms)
     *
     * Connection Pool Sizing:
     * - Formula: connections ≈ (concurrent requests × avg query time) / 1000
     * - Example: 500 RPS × 10ms avg = 5 connections needed
     * - Set maxPoolSize = 2-3x peak requirement for bursts
     * - Low traffic: maxPoolSize=20
     * - Medium traffic: maxPoolSize=50 (default)
     * - High traffic: maxPoolSize=100
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientSettings() {
        return builder -> builder
                // ═══════════════════════════════════════════════════════════
                // P0 FIX: Write Concern - MAJORITY + JOURNALED
                // ═══════════════════════════════════════════════════════════
                .writeConcern(WriteConcern.MAJORITY
                        .withJournal(true)
                        .withWTimeout(5, TimeUnit.SECONDS))

                // ═══════════════════════════════════════════════════════════
                // P0 FIX: Read Preference - PRIMARY_PREFERRED
                // ═══════════════════════════════════════════════════════════
                .readPreference(ReadPreference.primaryPreferred())

                // ═══════════════════════════════════════════════════════════
                // P0 FIX: Read Concern - MAJORITY
                // ═══════════════════════════════════════════════════════════
                .readConcern(ReadConcern.MAJORITY)

                // ═══════════════════════════════════════════════════════════
                // P1 FIX: Connection Pool Configuration
                // ═══════════════════════════════════════════════════════════
                .applyToConnectionPoolSettings(poolSettings ->
                        poolSettings
                                // Max connections in the pool
                                .maxSize(50)

                                // Min connections (kept warm)
                                .minSize(10)

                                // Max time a connection can be idle (60s)
                                .maxConnectionIdleTime(60, TimeUnit.SECONDS)

                                // Max time for a connection to be alive (30 min)
                                // Helps with load balancer connection rotation
                                .maxConnectionLifeTime(30, TimeUnit.MINUTES)

                                // Max wait time for connection from pool (10s)
                                .maxWaitTime(10, TimeUnit.SECONDS)

                                // Max simultaneous connection creation (prevents storms)
                                .maxConnecting(2)
                )

                // ═══════════════════════════════════════════════════════════
                // P1 FIX: Socket and Server Selection Timeouts
                // ═══════════════════════════════════════════════════════════
                .applyToSocketSettings(socketSettings ->
                        socketSettings
                                // Socket connection timeout (10s)
                                .connectTimeout(10, TimeUnit.SECONDS)

                                // Socket read timeout (30s)
                                .readTimeout(30, TimeUnit.SECONDS)
                )

                .applyToClusterSettings(clusterSettings ->
                        clusterSettings
                                // Server selection timeout (30s)
                                .serverSelectionTimeout(30, TimeUnit.SECONDS)
                );
    }

    /**
     * Converter: BigDecimal → Decimal128 (for writing to MongoDB)
     */
    static class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
        @Override
        public Decimal128 convert(BigDecimal source) {
            return new Decimal128(source);
        }
    }

    /**
     * Converter: Decimal128 → BigDecimal (for reading from MongoDB)
     */
    static class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
        @Override
        public BigDecimal convert(Decimal128 source) {
            return source.bigDecimalValue();
        }
    }
}

