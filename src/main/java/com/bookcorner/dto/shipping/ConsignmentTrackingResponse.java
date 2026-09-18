package com.bookcorner.dto.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Consignment tracking milestones and transit status timeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsignmentTrackingResponse implements Serializable {

    private String trackingNumber;
    private String carrierCode;
    private String consignmentStatus;
    private Instant estimatedDeliveryAt;
    private Instant actualDeliveryAt;

    @Builder.Default
    private List<MilestoneDto> milestones = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneDto implements Serializable {
        private String status;
        private String location;
        private Instant timestamp;
        private String description;
    }
}
