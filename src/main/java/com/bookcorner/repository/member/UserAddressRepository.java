package com.bookcorner.repository.member;

import com.bookcorner.entity.member.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserAddressEntity.
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, UUID> {

    /**
     * Retrieve all addresses belonging to a user.
     */
    List<UserAddressEntity> findByUserId(UUID userId);

    /**
     * Retrieve a specific address verifying user ownership.
     */
    Optional<UserAddressEntity> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find default address for user by address type (SHIPPING, BILLING).
     */
    Optional<UserAddressEntity> findByUserIdAndAddressTypeAndIsDefaultTrue(UUID userId, String addressType);

    /**
     * Reset default status on existing addresses before setting a new default.
     */
    @Modifying
    @Query("""
        UPDATE UserAddressEntity ua 
        SET ua.isDefault = false,
            ua.version = ua.version + 1
        WHERE ua.user.id = :userId 
          AND ua.addressType = :addressType
        """)
    void clearDefaultFlags(@Param("userId") UUID userId, @Param("addressType") String addressType);
}
