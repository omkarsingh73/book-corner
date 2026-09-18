package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a promotional coupon is expired, exceeds maximum redemption limits,
 * or does not satisfy minimum order amount constraints.
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidCouponException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "INVALID_COUPON";

    private final String couponCode;
    private final String reason;

    public InvalidCouponException(String couponCode, String reason) {
        super(String.format("Coupon '%s' cannot be applied: %s", couponCode, reason),
                DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.couponCode = couponCode;
        this.reason = reason;
    }

    public InvalidCouponException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.couponCode = null;
        this.reason = message;
    }
}
