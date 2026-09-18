package com.bookcorner.dto.payment;

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
 * Coordinated payment intent response returned to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse implements Serializable {

    private UUID paymentTransactionId;
    private String orderNumber;
    private String transactionStatus;
    private String clientSecret;
    private String gatewayProvider;
    private MoneyDto totalPayableAmount;
    private String idempotencyKey;

    @Builder.Default
    private List<TenderSplitSummary> splits = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenderSplitSummary implements Serializable {
        private String tenderType;
        private MoneyDto amount;
        private String status;
    }
}
