package com.bookcorner.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cross-sell bundle and companion titles response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellResponse implements Serializable {

    private UUID sourceBookId;
    private Double bundleDiscountPercentage;

    @Builder.Default
    private List<CompanionTitleSummary> companionTitles = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanionTitleSummary implements Serializable {
        private UUID bookId;
        private String title;
        private String authorName;
        private String formatType;
        private Long individualPrice;
        private Long bundlePrice;
        private String coverImageUrl;
    }
}
