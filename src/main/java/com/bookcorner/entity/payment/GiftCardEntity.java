package com.bookcorner.entity.payment;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * JPA entity representing a pre-loaded electronic or physical gift card.
 * Maps to payment.gift_cards table.
 */
@Entity
@Table(name = "gift_cards", schema = "payment")
@SQLDelete(sql = "UPDATE payment.gift_cards SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GiftCardEntity extends BaseAuditEntity {

    @Column(name = "card_number", nullable = false, length = 32, unique = true)
    private String cardNumber;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "initial_balance", nullable = false)
    private Long initialBalance;

    @Column(name = "current_balance", nullable = false)
    private Long currentBalance;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
