package com.bookcorner.entity.payment;

import com.bookcorner.common.persistence.BaseAuditEntity;
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

/**
 * JPA entity representing a multi-tender split portion (Credit Card, Wallet, Gift Card).
 * Maps to payment.tender_splits table.
 */
@Entity
@Table(name = "tender_splits", schema = "payment")
@SQLDelete(sql = "UPDATE payment.tender_splits SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TenderSplitEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransactionEntity paymentTransaction;

    @Column(name = "tender_type", nullable = false, length = 32)
    private String tenderType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "tender_reference", length = 255)
    private String tenderReference;

    @Column(name = "tender_status", nullable = false, length = 32)
    @Builder.Default
    private String tenderStatus = "AUTHORIZED";
}
