package com.bookcorner.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to apply a discount coupon code to shopping cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest implements Serializable {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 64, message = "Coupon code must be between 3 and 64 characters")
    private String couponCode;
}
