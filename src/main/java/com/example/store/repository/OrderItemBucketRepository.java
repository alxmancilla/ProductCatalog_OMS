package com.example.store.repository;

import com.example.store.model.OrderItemBucket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository for OrderItemBucket entities.
 * 
 * Provides methods to query order item buckets for large orders.
 */
public interface OrderItemBucketRepository extends MongoRepository<OrderItemBucket, String> {
    
    /**
     * Find all buckets for a specific order, sorted by bucket number.
     * 
     * @param orderId the order ID
     * @return list of buckets sorted by bucketNumber
     */
    List<OrderItemBucket> findByOrderIdOrderByBucketNumberAsc(String orderId);
    
    /**
     * Find a specific bucket for an order.
     * 
     * @param orderId the order ID
     * @param bucketNumber the bucket number
     * @return the bucket, or null if not found
     */
    OrderItemBucket findByOrderIdAndBucketNumber(String orderId, Integer bucketNumber);
    
    /**
     * Delete all buckets for a specific order.
     * 
     * @param orderId the order ID
     */
    void deleteByOrderId(String orderId);
}

