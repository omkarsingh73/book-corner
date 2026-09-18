package com.bookcorner.dto.order;

import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed order invoice specification response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse implements Serializable {

    private String orderNumber;
    private String orderStatus;
    private PricingBreakdown pricing;
    private AddressDto shippingAddress;
    private AddressDto billingAddress;
    private Instant placedAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Boolean isCancellable;

    @Builder.Default
    private List<OrderLineItemDto> lineItems = new ArrayList<>();

    @Builder.Default
    private List<StatusHistoryDto> timeline = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingBreakdown implements Serializable {
        private MoneyDto subtotal;
        private MoneyDto discount;
        private MoneyDto shipping;
        private MoneyDto tax;
        private MoneyDto total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemDto implements Serializable {
        private UUID lineItemId;
        private UUID formatId;
        private String bookTitle;
        private String isbn13;
        private String formatType;
        private MoneyDto unitPrice;
        private Integer quantity;
        private MoneyDto lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryDto implements Serializable {
        private String fromStatus;
        private String toStatus;
        private String notes;
        private Instant timestamp;
    }
}
