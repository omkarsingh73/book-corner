package com.bookcorner.repository;

import com.bookcorner.config.AbstractPostgresRepositoryTest;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.repository.ordering.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CouponRepository Integration Tests (PostgreSQL Testcontainers)")
class CouponRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    private CouponEntity validCoupon;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();

        validCoupon = couponRepository.save(CouponEntity.builder()
                .couponCode("SUMMER20")
                .discountType("PERCENTAGE")
                .discountValue(20L)
                .minimumOrderAmount(3000L)
                .maxDiscountAmount(1500L)
                .maxRedemptions(100)
                .currentRedemptions(10)
                .validFrom(Instant.now().minus(5, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(5, ChronoUnit.DAYS))
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Should find coupon by promotional code")
    void shouldFindByCouponCode() {
        Optional<CouponEntity> found = couponRepository.findByCouponCode("SUMMER20");

        assertThat(found).isPresent();
        assertThat(found.get().getCouponCode()).isEqualTo("SUMMER20");
        assertThat(found.get().getDiscountType()).isEqualTo("PERCENTAGE");
        assertThat(found.get().getDiscountValue()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should find valid coupon within active time window and usage limits")
    void shouldFindValidCoupon() {
        Optional<CouponEntity> valid = couponRepository.findValidCoupon("SUMMER20", Instant.now());
        assertThat(valid).isPresent();

        // Expired coupon
        CouponEntity expiredCoupon = couponRepository.save(CouponEntity.builder()
                .couponCode("EXPIRED50")
                .discountType("PERCENTAGE")
                .discountValue(50L)
                .validFrom(Instant.now().minus(20, ChronoUnit.DAYS))
                .validUntil(Instant.now().minus(1, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        Optional<CouponEntity> expired = couponRepository.findValidCoupon("EXPIRED50", Instant.now());
        assertThat(expired).isEmpty();
    }

    @Test
    @DisplayName("Should increment redemption count on coupon usage")
    void shouldIncrementRedemptions() {
        int updatedRows = couponRepository.incrementRedemptions(validCoupon.getId());
        assertThat(updatedRows).isEqualTo(1);

        Optional<CouponEntity> reloaded = couponRepository.findById(validCoupon.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getCurrentRedemptions()).isEqualTo(11);
    }
}
