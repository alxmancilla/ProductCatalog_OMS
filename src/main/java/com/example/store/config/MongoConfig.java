package com.example.store.config;

import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
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

