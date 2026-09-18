package com.bookcorner.dto.review;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to cast a helpfulness vote on a review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteReviewRequest implements Serializable {

    @NotNull(message = "isHelpful vote flag is required")
    private Boolean isHelpful;
}
