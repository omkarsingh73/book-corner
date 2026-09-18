package com.bookcorner.dto.payment;

import com.bookcorner.dto.common.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload to coordinate a multi-tender payment session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest implements Serializable {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotEmpty(message = "At least one tender split must be specified")
    @Valid
    @Builder.Default
    private List<TenderSplitRequest> tenderSplits = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenderSplitRequest implements Serializable {
        @NotBlank(message = "Tender type is required (CREDIT_CARD, WALLET, GIFT_CARD)")
        private String tenderType;

        @Valid
        private MoneyDto amount;

        private String tenderReference;
    }
}
