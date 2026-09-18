package com.bookcorner.dto.order;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Compact order summary card for customer order history dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto implements Serializable {

    private String orderNumber;
    private String orderStatus;
    private MoneyDto totalAmount;
    private Integer itemCount;
    private Instant placedAt;
}
