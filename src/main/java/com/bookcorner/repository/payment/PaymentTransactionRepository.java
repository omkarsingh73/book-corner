package com.bookcorner.repository.payment;

import com.bookcorner.entity.payment.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PaymentTransactionEntity.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    /**
     * Find transaction by cryptographic idempotency key to prevent double-charging.
     */
    Optional<PaymentTransactionEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find transaction by external gateway transaction ID (e.g. Stripe pi_xxx).
     */
    Optional<PaymentTransactionEntity> findByGatewayTransactionId(String gatewayTransactionId);

    /**
     * Find transaction with all tender splits eagerly loaded.
     */
    @EntityGraph(attributePaths = {"tenderSplits"})
    @Query("SELECT pt FROM PaymentTransactionEntity pt WHERE pt.id = :id")
    Optional<PaymentTransactionEntity> findByIdWithSplits(@Param("id") UUID id);

    /**
     * Find all transactions for a specific order with tender splits eagerly loaded.
     */
    @EntityGraph(attributePaths = {"tenderSplits"})
    List<PaymentTransactionEntity> findByOrderId(UUID orderId);
}
