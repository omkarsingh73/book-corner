package com.bookcorner.entity.payment;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.member.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a customer's stored digital credit balance.
 * Maps to payment.customer_wallets table.
 */
@Entity
@Table(name = "customer_wallets", schema = "payment")
@SQLDelete(sql = "UPDATE payment.customer_wallets SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerWalletEntity extends BaseAuditEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "current_balance", nullable = false)
    @Builder.Default
    private Long currentBalance = 0L;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<WalletLedgerEntryEntity> ledgerEntries = new ArrayList<>();

    public void addLedgerEntry(String entryType, long delta, String referenceId, String notes) {
        this.currentBalance += delta;
        WalletLedgerEntryEntity entry = WalletLedgerEntryEntity.builder()
                .wallet(this)
                .entryType(entryType)
                .amount(delta)
                .balanceAfter(this.currentBalance)
                .referenceId(referenceId)
                .notes(notes)
                .build();
        ledgerEntries.add(entry);
    }
}
