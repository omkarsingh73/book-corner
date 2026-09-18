package com.bookcorner.repository.payment;

import com.bookcorner.entity.payment.RefundRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for RefundRecordEntity.
 */
@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecordEntity, UUID> {

    /**
     * Find all refund records for a specific order.
     */
    List<RefundRecordEntity> findByOrderId(UUID orderId);

    /**
     * Find refunds by processing status with pagination.
     */
    Page<RefundRecordEntity> findByRefundStatus(String refundStatus, Pageable pageable);
}
