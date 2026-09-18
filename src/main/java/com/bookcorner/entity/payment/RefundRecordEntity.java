package com.bookcorner.entity.payment;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * JPA entity representing a credit refund audit record for cancelled or returned orders.
 * Maps to payment.refund_records table.
 */
@Entity
@Table(name = "refund_records", schema = "payment")
@SQLDelete(sql = "UPDATE payment.refund_records SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefundRecordEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransactionEntity paymentTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "refund_reason", nullable = false, length = 255)
    private String refundReason;

    @Column(name = "gateway_refund_id", length = 255)
    private String gatewayRefundId;

    @Column(name = "refund_status", nullable = false, length = 32)
    @Builder.Default
    private String refundStatus = "PENDING";

    @Column(name = "processed_at")
    private Instant processedAt;
}
