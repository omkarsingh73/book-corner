package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CouponEntity.
 */
@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

    /**
     * Find coupon by unique promotional code.
     */
    Optional<CouponEntity> findByCouponCode(String couponCode);

    /**
     * Find active, currently valid coupon within its time window and redemption limit.
     */
    @Query("""
        SELECT c FROM CouponEntity c 
        WHERE c.couponCode = :code 
          AND c.isActive = true 
          AND c.validFrom <= :now 
          AND c.validUntil >= :now
          AND (c.maxRedemptions IS NULL OR c.currentRedemptions < c.maxRedemptions)
        """)
    Optional<CouponEntity> findValidCoupon(@Param("code") String code, @Param("now") Instant now);

    /**
     * Atomically increment the redemption count of a coupon upon order checkout completion.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CouponEntity c 
        SET c.currentRedemptions = c.currentRedemptions + 1,
            c.version = c.version + 1
        WHERE c.id = :couponId 
          AND (c.maxRedemptions IS NULL OR c.currentRedemptions < c.maxRedemptions)
        """)
    int incrementRedemptions(@Param("couponId") UUID couponId);
}
