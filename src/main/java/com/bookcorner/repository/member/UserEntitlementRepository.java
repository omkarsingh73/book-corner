package com.bookcorner.repository.member;

import com.bookcorner.entity.member.UserEntitlementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserEntitlementEntity (digital content rights).
 */
@Repository
public interface UserEntitlementRepository extends JpaRepository<UserEntitlementEntity, UUID> {

    /**
     * Find active digital entitlement for user and format SKU.
     */
    @EntityGraph(attributePaths = {"format.book"})
    Optional<UserEntitlementEntity> findByUserIdAndFormatId(UUID userId, UUID formatId);

    /**
     * Check if a customer owns a digital entitlement to a format SKU.
     */
    boolean existsByUserIdAndFormatId(UUID userId, UUID formatId);

    /**
     * Retrieve all digital entitlements owned by a customer.
     */
    @EntityGraph(attributePaths = {"format.book"})
    Page<UserEntitlementEntity> findByUserId(UUID userId, Pageable pageable);

    /**
     * Increment download count for digital content.
     */
    @Modifying
    @Query("""
        UPDATE UserEntitlementEntity ue 
        SET ue.downloadCount = ue.downloadCount + 1,
            ue.version = ue.version + 1
        WHERE ue.id = :id
        """)
    void incrementDownloadCount(@Param("id") UUID id);
}
