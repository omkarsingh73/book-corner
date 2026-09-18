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

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an anonymous guest shopping session.
 * Maps to member.guest_sessions table.
 */
@Entity
@Table(name = "guest_sessions", schema = "member")
@SQLDelete(sql = "UPDATE member.guest_sessions SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GuestSessionEntity extends BaseAuditEntity {

    @Column(name = "session_token", nullable = false, length = 128, unique = true)
    private String sessionToken;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
