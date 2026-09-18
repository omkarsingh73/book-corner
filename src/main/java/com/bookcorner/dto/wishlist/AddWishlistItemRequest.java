package com.bookcorner.dto.wishlist;

import com.bookcorner.dto.common.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Payload to add a book to customer wishlist with an optional price-drop alert threshold.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWishlistItemRequest implements Serializable {

    @NotNull(message = "Book ID is required")
    private UUID bookId;

    @Valid
    private MoneyDto desiredPriceAlert;
}
