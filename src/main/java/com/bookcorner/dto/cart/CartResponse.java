package com.bookcorner.dto.cart;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed shopping cart response representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse implements Serializable {

    private UUID cartId;
    private MoneyDto subtotal;
    private MoneyDto discount;
    private MoneyDto estimatedShipping;
    private MoneyDto estimatedTax;
    private MoneyDto total;
    private String couponCode;
    private Integer totalQuantity;

    @Builder.Default
    private List<CartLineItemDto> items = new ArrayList<>();
}
