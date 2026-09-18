package com.bookcorner.entity.store;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
 * JPA entity representing configurable store business policies.
 * Maps to store.store_policies table.
 */
@Entity
@Table(name = "store_policies", schema = "store")
@SQLDelete(sql = "UPDATE store.store_policies SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StorePolicyEntity extends BaseAuditEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private StoreEntity store;

    @Column(name = "cancellation_grace_minutes", nullable = false)
    @Builder.Default
    private int cancellationGraceMinutes = 60;

    @Column(name = "return_window_days", nullable = false)
    @Builder.Default
    private int returnWindowDays = 30;

    @Column(name = "free_shipping_threshold_amount", nullable = false)
    @Builder.Default
    private Long freeShippingThresholdAmount = 5000L;
}
