package com.bookcorner.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to create a new address in customer address book.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest implements Serializable {

    @NotBlank(message = "Address type is required (SHIPPING or BILLING)")
    @Builder.Default
    private String addressType = "SHIPPING";

    private Boolean isDefault;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150)
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must conform to E.164 format")
    private String phoneNumber;

    @NotBlank(message = "Street address line 1 is required")
    @Size(max = 255)
    private String streetAddress1;

    @Size(max = 255)
    private String streetAddress2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State or Province is required")
    @Size(max = 100)
    private String stateProvince;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    private String postalCode;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2-letter ISO alpha-2")
    private String countryCode;

    @Size(max = 500)
    private String deliveryInstructions;
}
