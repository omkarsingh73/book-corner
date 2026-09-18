package com.bookcorner.dto.review;

import com.bookcorner.dto.common.PageMetaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Paginated book reviews collection response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewsResponse implements Serializable {

    private UUID bookId;
    private Double averageRating;
    private Integer totalReviews;

    @Builder.Default
    private List<ReviewDto> data = new ArrayList<>();

    private PageMetaDto pagination;
}
