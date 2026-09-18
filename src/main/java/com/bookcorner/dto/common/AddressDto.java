package com.bookcorner.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Standard postal and delivery address representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto implements Serializable {

    private UUID id;

    @NotBlank(message = "Address type is required (e.g. SHIPPING, BILLING)")
    @Builder.Default
    private String addressType = "SHIPPING";

    private Boolean isDefault;

    @NotBlank(message = "Recipient full name is required")
    @Size(max = 150, message = "Recipient name cannot exceed 150 characters")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must conform to E.164 format")
    private String phoneNumber;

    @NotBlank(message = "Street address line 1 is required")
    @Size(max = 255, message = "Street address line 1 cannot exceed 255 characters")
    private String streetAddress1;

    @Size(max = 255, message = "Street address line 2 cannot exceed 255 characters")
    private String streetAddress2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "State/Province is required")
    @Size(max = 100, message = "State/Province cannot exceed 100 characters")
    private String stateProvince;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be a 2-letter ISO-3166-1 alpha-2 code")
    private String countryCode;

    @Size(max = 500, message = "Delivery instructions cannot exceed 500 characters")
    private String deliveryInstructions;

    public String getStreetLine1() {
        return streetAddress1;
    }

    public void setStreetLine1(String streetLine1) {
        this.streetAddress1 = streetLine1;
    }

    public String getStreetLine2() {
        return streetAddress2;
    }

    public void setStreetLine2(String streetLine2) {
        this.streetAddress2 = streetLine2;
    }

    public String getStateOrProvince() {
        return stateProvince;
    }

    public void setStateOrProvince(String stateOrProvince) {
        this.stateProvince = stateOrProvince;
    }

    public static class AddressDtoBuilder {
        public AddressDtoBuilder streetLine1(String streetLine1) {
            this.streetAddress1 = streetLine1;
            return this;
        }

        public AddressDtoBuilder streetLine2(String streetLine2) {
            this.streetAddress2 = streetLine2;
            return this;
        }

        public AddressDtoBuilder stateOrProvince(String stateOrProvince) {
            this.stateProvince = stateOrProvince;
            return this;
        }
    }
}
