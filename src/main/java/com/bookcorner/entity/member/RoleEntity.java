package com.bookcorner.entity.member;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * JPA entity representing user security roles (CUSTOMER, STORE_ADMIN, CATALOG_MANAGER).
 * Maps to member.roles table.
 */
@Entity
@Table(name = "roles", schema = "member")
@SQLDelete(sql = "UPDATE member.roles SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoleEntity extends BaseAuditEntity {

    @Column(name = "role_code", nullable = false, length = 64, unique = true)
    private String roleCode;

    @Column(name = "description", length = 255)
    private String description;
}
