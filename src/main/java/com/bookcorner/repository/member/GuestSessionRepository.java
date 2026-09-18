package com.bookcorner.repository.member;

import com.bookcorner.entity.member.GuestSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for GuestSessionEntity.
 */
@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSessionEntity, UUID> {

    /**
     * Find guest session by session token.
     */
    Optional<GuestSessionEntity> findBySessionToken(String sessionToken);

    /**
     * Find active unexpired guest session.
     */
    @Query("""
        SELECT gs FROM GuestSessionEntity gs 
        WHERE gs.sessionToken = :token 
          AND gs.expiresAt > :now
        """)
    Optional<GuestSessionEntity> findActiveSession(@Param("token") String token, @Param("now") Instant now);

    /**
     * Delete expired guest sessions for maintenance cleanup.
     */
    @Modifying
    @Query("DELETE FROM GuestSessionEntity gs WHERE gs.expiresAt < :cutoff")
    int deleteExpiredSessions(@Param("cutoff") Instant cutoff);
}
