package com.bookcorner.dto.cart;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Line item representation within a shopping cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartLineItemDto implements Serializable {

    private UUID itemId;
    private UUID formatId;
    private UUID bookId;
    private String bookTitle;
    private String authorNames;
    private String coverImageUrl;
    private String formatType;
    private String sku;
    private MoneyDto unitPrice;
    private Integer quantity;
    private MoneyDto lineTotal;
}
