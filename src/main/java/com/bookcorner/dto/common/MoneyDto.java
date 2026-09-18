package com.bookcorner.dto.common;

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
 * Standard monetary amount representation with atomic integer currency cents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyDto implements Serializable {

    @NotNull(message = "Amount in cents is required")
    @Min(value = 0, message = "Amount cannot be negative")
    private Long amount;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be a 3-letter ISO-4217 code")
    @Builder.Default
    private String currency = "USD";

    public static MoneyDto of(long cents, String currency) {
        return MoneyDto.builder()
                .amount(cents)
                .currency(currency != null ? currency : "USD")
                .build();
    }
}
