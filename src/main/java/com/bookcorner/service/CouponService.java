package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.repository.ordering.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Promotional Coupon and Discount Service.
 * Manages promotional lifecycle, qualification rules, discount computations, and redemptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Validates a promotional coupon code against business eligibility rules.
     *
     * @param couponCode promotional coupon code string
     * @param orderSubtotalCents order basket subtotal in cents
     * @return validated CouponEntity
     */
    @Transactional(readOnly = true)
    public CouponEntity validateCoupon(String couponCode, long orderSubtotalCents) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new BusinessRuleViolationException("Coupon code cannot be empty.");
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        log.info("Validating promotional coupon: {} for subtotal: {} cents", normalizedCode, orderSubtotalCents);

        CouponEntity coupon = couponRepository.findByCouponCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotional coupon not found: " + normalizedCode));

        if (!coupon.isActive()) {
            log.warn("Coupon {} is inactive", normalizedCode);
            throw new BusinessRuleViolationException("Coupon " + normalizedCode + " is no longer active.");
        }

        Instant now = Instant.now();
        if (coupon.getValidFrom().isAfter(now)) {
            log.warn("Coupon {} is not yet active (valid from: {})", normalizedCode, coupon.getValidFrom());
            throw new BusinessRuleViolationException("Coupon " + normalizedCode + " is not active yet.");
        }

        if (coupon.getValidUntil().isBefore(now)) {
            log.warn("Coupon {} has expired (valid until: {})", normalizedCode, coupon.getValidUntil());
            throw new BusinessRuleViolationException("Coupon " + normalizedCode + " has expired.");
        }

        if (coupon.getMaxRedemptions() != null && coupon.getCurrentRedemptions() >= coupon.getMaxRedemptions()) {
            log.warn("Coupon {} reached max redemptions ({}/{})", 
                    normalizedCode, coupon.getCurrentRedemptions(), coupon.getMaxRedemptions());
            throw new BusinessRuleViolationException("Coupon " + normalizedCode + " has reached its maximum redemption limit.");
        }

        if (orderSubtotalCents < coupon.getMinimumOrderAmount()) {
            log.warn("Subtotal {} cents does not satisfy coupon {} minimum order amount {} cents",
                    orderSubtotalCents, normalizedCode, coupon.getMinimumOrderAmount());
            throw new BusinessRuleViolationException(String.format(
                    "Order subtotal of %.2f does not meet minimum order requirement of %.2f for coupon %s.",
                    orderSubtotalCents / 100.0, coupon.getMinimumOrderAmount() / 100.0, normalizedCode));
        }

        log.info("Promotional coupon {} successfully validated.", normalizedCode);
        return coupon;
    }

    /**
     * Calculates the monetary discount amount in cents given a valid coupon and subtotal.
     *
     * @param coupon validated coupon entity
     * @param orderSubtotalCents subtotal amount in cents
     * @return calculated discount in cents (never exceeds subtotal)
     */
    public long calculateDiscount(CouponEntity coupon, long orderSubtotalCents) {
        if (coupon == null || orderSubtotalCents <= 0) {
            return 0L;
        }

        long discountCents = 0L;

        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            discountCents = (orderSubtotalCents * coupon.getDiscountValue()) / 100L;
            if (coupon.getMaxDiscountAmount() != null && discountCents > coupon.getMaxDiscountAmount()) {
                discountCents = coupon.getMaxDiscountAmount();
            }
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(coupon.getDiscountType())) {
            discountCents = coupon.getDiscountValue();
        } else {
            log.warn("Unknown discount type: {} for coupon ID: {}", coupon.getDiscountType(), coupon.getId());
            return 0L;
        }

        // Discount cannot exceed the subtotal
        long finalDiscount = Math.min(discountCents, orderSubtotalCents);
        log.debug("Calculated discount for coupon {}: {} cents (from subtotal: {} cents)",
                coupon.getCouponCode(), finalDiscount, orderSubtotalCents);
        return finalDiscount;
    }

    /**
     * Atomically records coupon redemption by incrementing current usage count.
     *
     * @param couponId UUID of the coupon to redeem
     */
    @Transactional
    public void redeemCoupon(UUID couponId) {
        log.info("Redeeming promotional coupon ID: {}", couponId);
        int rowsUpdated = couponRepository.incrementRedemptions(couponId);
        if (rowsUpdated == 0) {
            log.error("Failed to redeem coupon ID {}: redemption limit reached or inactive", couponId);
            throw new BusinessRuleViolationException("Unable to redeem coupon: limit reached or coupon inactive.");
        }
        log.info("Coupon ID {} successfully redeemed.", couponId);
    }
}
