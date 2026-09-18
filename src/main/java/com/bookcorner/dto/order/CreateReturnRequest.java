package com.bookcorner.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload to initiate a Return Merchandise Authorization (RMA) request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest implements Serializable {

    @NotBlank(message = "Reason code is required (DAMAGED_ITEM, WRONG_ITEM, CHANGED_MIND, POOR_QUALITY)")
    private String reasonCode;

    private String customerRemarks;

    @NotEmpty(message = "At least one item must be specified for return")
    @Valid
    @Builder.Default
    private List<ReturnItemRequest> returnItems = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest implements Serializable {
        @NotNull(message = "Order line item ID is required")
        private UUID orderLineItemId;

        @NotNull(message = "Return quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
