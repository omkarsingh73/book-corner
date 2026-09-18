package com.bookcorner.entity.shipping;

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

/**
 * JPA entity representing freight rate calculations and carrier delivery SLAs.
 * Maps to shipping.carrier_rate_cards table.
 */
@Entity
@Table(name = "carrier_rate_cards", schema = "shipping")
@SQLDelete(sql = "UPDATE shipping.carrier_rate_cards SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CarrierRateCardEntity extends BaseAuditEntity {

    @Column(name = "carrier_code", nullable = false, length = 64)
    private String carrierCode;

    @Column(name = "service_level", nullable = false, length = 64)
    private String serviceLevel;

    @Column(name = "origin_country_code", nullable = false, length = 2)
    @Builder.Default
    private String originCountryCode = "US";

    @Column(name = "destination_country_code", nullable = false, length = 2)
    @Builder.Default
    private String destinationCountryCode = "US";

    @Column(name = "base_rate_amount", nullable = false)
    private Long baseRateAmount;

    @Column(name = "per_kg_rate_amount", nullable = false)
    @Builder.Default
    private Long perKgRateAmount = 0L;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "estimated_min_days", nullable = false)
    @Builder.Default
    private int estimatedMinDays = 1;

    @Column(name = "estimated_max_days", nullable = false)
    @Builder.Default
    private int estimatedMaxDays = 5;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
