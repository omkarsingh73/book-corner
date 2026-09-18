package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.repository.ordering.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService Unit Tests (Mockito & AssertJ)")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private UUID couponId;
    private CouponEntity activePercentCoupon;
    private CouponEntity activeFixedCoupon;

    @BeforeEach
    void setUp() {
        couponId = UUID.randomUUID();

        activePercentCoupon = CouponEntity.builder()
                .id(couponId)
                .couponCode("SAVE20")
                .discountType("PERCENTAGE")
                .discountValue(20L)
                .minimumOrderAmount(3000L) // $30.00 minimum
                .maxDiscountAmount(1500L) // $15.00 max discount cap
                .currentRedemptions(5)
                .maxRedemptions(100)
                .isActive(true)
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        activeFixedCoupon = CouponEntity.builder()
                .id(UUID.randomUUID())
                .couponCode("FLAT10")
                .discountType("FIXED_AMOUNT")
                .discountValue(1000L) // $10.00
                .minimumOrderAmount(2500L) // $25.00
                .currentRedemptions(0)
                .maxRedemptions(50)
                .isActive(true)
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("Should validate coupon successfully when all criteria are met")
    void shouldValidateCouponSuccessfully() {
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        CouponEntity result = couponService.validateCoupon("SAVE20", 5000L);

        assertThat(result).isNotNull();
        assertThat(result.getCouponCode()).isEqualTo("SAVE20");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when coupon code is blank")
    void shouldThrowExceptionWhenCouponCodeIsBlank() {
        assertThatThrownBy(() -> couponService.validateCoupon("  ", 5000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when coupon code does not exist")
    void shouldThrowResourceNotFoundExceptionWhenCouponNotFound() {
        when(couponRepository.findByCouponCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateCoupon("UNKNOWN", 5000L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Promotional coupon not found");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when coupon is deactivated")
    void shouldThrowExceptionWhenCouponIsInactive() {
        activePercentCoupon.setActive(false);
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("SAVE20", 5000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when coupon valid window is in the future")
    void shouldThrowExceptionWhenCouponNotYetActive() {
        activePercentCoupon.setValidFrom(Instant.now().plus(2, ChronoUnit.DAYS));
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("SAVE20", 5000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not active yet");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when coupon has expired")
    void shouldThrowExceptionWhenCouponExpired() {
        activePercentCoupon.setValidUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("SAVE20", 5000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has expired");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when redemption limit reached")
    void shouldThrowExceptionWhenMaxRedemptionsExceeded() {
        activePercentCoupon.setCurrentRedemptions(100);
        activePercentCoupon.setMaxRedemptions(100);
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("SAVE20", 5000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("reached its maximum redemption limit");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when subtotal is below minimum order amount")
    void shouldThrowExceptionWhenSubtotalBelowMinimum() {
        when(couponRepository.findByCouponCode("SAVE20")).thenReturn(Optional.of(activePercentCoupon));

        // Minimum order amount is 3000 cents ($30), pass 2000 cents ($20)
        assertThatThrownBy(() -> couponService.validateCoupon("SAVE20", 2000L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not meet minimum order requirement");
    }

    @Test
    @DisplayName("Should calculate percentage discount accurately and cap at max discount limit")
    void shouldCalculatePercentageDiscountWithCap() {
        // Subtotal = $100 (10,000 cents). 20% would be 2,000 cents.
        // But maxDiscountAmount is 1,500 cents ($15). Expected discount = 1500 cents.
        long discount = couponService.calculateDiscount(activePercentCoupon, 10000L);
        assertThat(discount).isEqualTo(1500L);

        // Subtotal = $50 (5,000 cents). 20% is 1,000 cents (below cap). Expected = 1000 cents.
        long discountBelowCap = couponService.calculateDiscount(activePercentCoupon, 5000L);
        assertThat(discountBelowCap).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Should calculate fixed amount discount accurately")
    void shouldCalculateFixedAmountDiscount() {
        // Subtotal = $50 (5000 cents), fixed discount = $10 (1000 cents)
        long discount = couponService.calculateDiscount(activeFixedCoupon, 5000L);
        assertThat(discount).isEqualTo(1000L);

        // If subtotal is smaller than fixed discount, discount is capped to subtotal
        long cappedDiscount = couponService.calculateDiscount(activeFixedCoupon, 800L);
        assertThat(cappedDiscount).isEqualTo(800L);
    }

    @Test
    @DisplayName("Should return 0 discount when coupon is null or subtotal is zero")
    void shouldReturnZeroDiscountForNullOrZeroSubtotal() {
        assertThat(couponService.calculateDiscount(null, 5000L)).isEqualTo(0L);
        assertThat(couponService.calculateDiscount(activePercentCoupon, 0L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should redeem coupon successfully when repository increments counter")
    void shouldRedeemCouponSuccessfully() {
        when(couponRepository.incrementRedemptions(couponId)).thenReturn(1);

        couponService.redeemCoupon(couponId);

        verify(couponRepository).incrementRedemptions(couponId);
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when coupon redemption increment fails")
    void shouldThrowExceptionWhenRedeemFails() {
        when(couponRepository.incrementRedemptions(couponId)).thenReturn(0);

        assertThatThrownBy(() -> couponService.redeemCoupon(couponId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Unable to redeem coupon");
    }
}
