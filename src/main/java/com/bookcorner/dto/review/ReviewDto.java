package com.bookcorner.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Verified customer book review representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto implements Serializable {

    private UUID reviewId;
    private UUID bookId;
    private String authorName;
    private Integer rating;
    private String reviewTitle;
    private String reviewBody;
    private Boolean isVerifiedPurchase;
    private Integer helpfulVotes;
    private Instant createdAt;
}
