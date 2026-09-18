package com.bookcorner.entity.ordering;

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
 * JPA entity representing a promotional discount coupon.
 * Maps to ordering.coupons table.
 */
@Entity
@Table(name = "coupons", schema = "ordering")
@SQLDelete(sql = "UPDATE ordering.coupons SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CouponEntity extends BaseAuditEntity {

    @Column(name = "coupon_code", nullable = false, length = 64, unique = true)
    private String couponCode;

    @Column(name = "discount_type", nullable = false, length = 32)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "minimum_order_amount", nullable = false)
    @Builder.Default
    private Long minimumOrderAmount = 0L;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "current_redemptions", nullable = false)
    @Builder.Default
    private int currentRedemptions = 0;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
