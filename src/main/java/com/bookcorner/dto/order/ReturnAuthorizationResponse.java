package com.bookcorner.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Return authorization and RMA shipping label response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnAuthorizationResponse implements Serializable {

    private String rmaNumber;
    private String rmaStatus;
    private String returnCarrier;
    private String returnTrackingNumber;
    private String prepaidReturnLabelUrl;
    private Long estimatedRefundAmount;
    private String returnInstructions;
}
