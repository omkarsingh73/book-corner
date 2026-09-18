package com.bookcorner.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Review vote acknowledgement matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteReviewResponse implements Serializable {

    private UUID reviewId;
    private Integer helpfulVotesCount;
    private Boolean userVote;
}
