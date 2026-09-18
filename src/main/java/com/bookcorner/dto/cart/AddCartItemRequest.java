package com.bookcorner.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Payload to add a book format SKU to shopping cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest implements Serializable {

    @NotNull(message = "Format SKU ID is required")
    private UUID formatId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 99, message = "Quantity cannot exceed 99 items per line")
    @Builder.Default
    private Integer quantity = 1;
}
