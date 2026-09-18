package com.bookcorner.dto.order;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Immediate synchronous order confirmation response returned upon checkout completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmationResponse implements Serializable {

    private String orderNumber;
    private String orderStatus;
    private MoneyDto totalAmount;
    private Instant placedAt;
    private String estimatedDelivery;
}
