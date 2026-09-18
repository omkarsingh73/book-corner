package com.bookcorner.entity.catalog;

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
 * JPA entity representing algorithmic or curated merchandising rules (UP-SELL, CROSS-SELL).
 * Maps to catalog.merchandising_rules table.
 */
@Entity
@Table(name = "merchandising_rules", schema = "catalog")
@SQLDelete(sql = "UPDATE catalog.merchandising_rules SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MerchandisingRuleEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_book_id", nullable = false)
    private BookEntity sourceBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_book_id", nullable = false)
    private BookEntity targetBook;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 1;

    @Column(name = "discount_bundle_pct")
    private Integer discountBundlePct;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
