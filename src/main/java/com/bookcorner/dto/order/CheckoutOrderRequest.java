package com.bookcorner.dto.order;

import com.bookcorner.dto.common.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Payload to execute order checkout saga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutOrderRequest implements Serializable {

    private UUID shippingAddressId;

    @Valid
    private AddressDto newShippingAddress;

    private UUID billingAddressId;

    @Valid
    private AddressDto newBillingAddress;

    @NotBlank(message = "Payment method token / nonce is required")
    private String paymentMethodToken;

    private String couponCode;

    private String customerNotes;
}
