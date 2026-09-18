package com.bookcorner.dto.search;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-faceted full-text search response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse implements Serializable {

    private String query;
    private Integer totalMatches;

    @Builder.Default
    private Map<String, Object> facets = new HashMap<>();

    @Builder.Default
    private List<SearchHitSummary> results = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchHitSummary implements Serializable {
        private UUID bookId;
        private String title;

        @Builder.Default
        private List<String> authorNames = new ArrayList<>();

        private String matchHighlight;
        private MoneyDto startingPrice;
        private Double averageRating;
    }
}
