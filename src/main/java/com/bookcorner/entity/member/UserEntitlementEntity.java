package com.bookcorner.entity.member;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
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
 * JPA entity representing digital content access entitlements (eBooks, Audiobooks).
 * Maps to member.user_entitlements table.
 */
@Entity
@Table(name = "user_entitlements", schema = "member")
@SQLDelete(sql = "UPDATE member.user_entitlements SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserEntitlementEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "format_id", nullable = false)
    private BookFormatEntity format;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "entitlement_type", nullable = false, length = 32)
    private String entitlementType;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private int downloadCount = 0;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "access_expires_at")
    private Instant accessExpiresAt;
}
