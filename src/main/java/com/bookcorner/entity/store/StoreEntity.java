package com.bookcorner.entity.store;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * JPA entity representing a multi-store tenant instance.
 * Maps to store.stores table.
 */
@Entity
@Table(name = "stores", schema = "store")
@SQLDelete(sql = "UPDATE store.stores SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StoreEntity extends BaseAuditEntity {

    @Column(name = "store_code", nullable = false, length = 64, unique = true)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String defaultCurrency = "USD";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private StorePolicyEntity policy;
}
