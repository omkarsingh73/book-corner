package com.bookcorner.dto.shipping;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to calculate dynamic carrier shipping rates and delivery estimates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateCalculationRequest implements Serializable {

    @NotBlank(message = "Destination postal code is required")
    private String destinationPostalCode;

    @NotBlank(message = "Destination country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2-letter ISO alpha-2")
    private String destinationCountryCode;

    @NotNull(message = "Package weight in grams is required")
    @Min(value = 1, message = "Weight must be at least 1 gram")
    private Integer weightGrams;
}
