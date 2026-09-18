package com.bookcorner.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to update attributes of an existing address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest implements Serializable {

    private Boolean isDefault;

    @Size(max = 150)
    private String recipientName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must conform to E.164 format")
    private String phoneNumber;

    @Size(max = 255)
    private String streetAddress1;

    @Size(max = 255)
    private String streetAddress2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String stateProvince;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 500)
    private String deliveryInstructions;
}
