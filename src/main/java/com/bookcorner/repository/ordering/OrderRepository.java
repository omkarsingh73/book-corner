package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for OrderEntity.
 * Supports JpaSpecificationExecutor for multi-criteria filtering,
 * EntityGraphs for complete invoice retrieval, and user-scoped pagination.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    /**
     * Find order by unique business order number (e.g. ORD-20260918-A8F2).
     */
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    /**
     * Find order with complete line items, status history, and payment transactions eagerly fetched.
     */
    @EntityGraph(attributePaths = {"lineItems.format.book", "statusHistory", "paymentTransactions", "coupon"})
    @Query("SELECT o FROM OrderEntity o WHERE o.orderNumber = :orderNumber")
    Optional<OrderEntity> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);

    /**
     * Find order by number enforcing user ownership.
     */
    Optional<OrderEntity> findByOrderNumberAndUserId(String orderNumber, UUID userId);

    /**
     * Retrieve paginated order history for a customer ordered by placement timestamp descending.
     */
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Retrieve customer orders filtered by status with pagination.
     */
    Page<OrderEntity> findByUserIdAndOrderStatusOrderByCreatedAtDesc(UUID userId, String orderStatus, Pageable pageable);

    /**
     * Count orders placed by customer.
     */
    long countByUserId(UUID userId);

    /**
     * Check if an order is eligible for self-service cancellation within grace period.
     */
    @Query("""
        SELECT COUNT(o) > 0 FROM OrderEntity o 
        WHERE o.orderNumber = :orderNumber 
          AND o.orderStatus IN ('PENDING_PAYMENT', 'CONFIRMED')
          AND o.placedAt >= :cutoffTime
        """)
    boolean isOrderCancellable(@Param("orderNumber") String orderNumber, @Param("cutoffTime") Instant cutoffTime);
}
