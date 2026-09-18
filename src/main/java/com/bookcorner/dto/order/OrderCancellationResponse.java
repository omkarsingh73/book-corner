package com.bookcorner.dto.order;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Order cancellation confirmation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancellationResponse implements Serializable {

    private String orderNumber;
    private String orderStatus;
    private Instant cancelledAt;
    private String cancellationReason;
    private Boolean refundInitiated;
    private MoneyDto refundAmount;
}
