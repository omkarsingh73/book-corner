package com.bookcorner.repository.member;

import com.bookcorner.entity.member.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserEntity.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Find user by email address with roles eagerly loaded for security authentication.
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findByEmail(String email);

    /**
     * Check if an active account exists with the given email address.
     */
    boolean existsByEmail(String email);

    /**
     * Find user by ID with full address book and roles loaded.
     */
    @EntityGraph(attributePaths = {"roles", "addresses"})
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithProfileDetails(@Param("id") UUID id);

    /**
     * Find users by account status with pagination.
     */
    Page<UserEntity> findByAccountStatus(String accountStatus, Pageable pageable);

    /**
     * Record successful login timestamp and reset failed attempts counter.
     */
    @Modifying
    @Query("""
        UPDATE UserEntity u 
        SET u.lastLoginAt = :loginTime, 
            u.failedLoginAttempts = 0,
            u.version = u.version + 1
        WHERE u.id = :userId
        """)
    void recordSuccessfulLogin(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime);

    /**
     * Increment failed login attempts counter.
     */
    @Modifying
    @Query("""
        UPDATE UserEntity u 
        SET u.failedLoginAttempts = u.failedLoginAttempts + 1,
            u.version = u.version + 1
        WHERE u.id = :userId
        """)
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);
}
