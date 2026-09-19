package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.CartEntity;
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
 * Spring Data JPA repository for CartEntity.
 */
@Repository
public interface CartRepository extends JpaRepository<CartEntity, UUID> {

    /**
     * Find active shopping cart for an authenticated user with items eagerly fetched.
     */
    @EntityGraph(attributePaths = {"items.format.book", "appliedCoupon"})
    Optional<CartEntity> findByUserId(UUID userId);

    /**
     * Find active shopping cart for an anonymous guest by session token.
     */
    @EntityGraph(attributePaths = {"items.format.book", "appliedCoupon"})
    @Query("""
        SELECT c FROM CartEntity c 
        WHERE c.guestSession.sessionToken = :sessionToken
        """)
    Optional<CartEntity> findByGuestSessionToken(@Param("sessionToken") String sessionToken);

    /**
     * Find cart for guest session ID.
     */
    Optional<CartEntity> findByGuestSessionId(UUID guestSessionId);

    /**
     * Check if a user already has an active cart.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Delete expired carts during scheduled maintenance cleanup.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartEntity c WHERE c.expiresAt < :cutoff")
    int deleteExpiredCarts(@Param("cutoff") Instant cutoff);
}
