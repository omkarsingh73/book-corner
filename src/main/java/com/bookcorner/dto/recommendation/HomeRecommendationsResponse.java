package com.bookcorner.dto.recommendation;

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
 * Personalized homepage recommendation carousel response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeRecommendationsResponse implements Serializable {

    private String strategy;

    @Builder.Default
    private List<RecommendedBookSummary> recommendations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedBookSummary implements Serializable {
        private UUID bookId;
        private String title;
        private String authorName;
        private String coverImageUrl;
        private MoneyDto startingPrice;
        private Double averageRating;
        private String recommendationReason;
    }
}
