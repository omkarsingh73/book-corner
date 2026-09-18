package com.bookcorner.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to submit a verified book review and rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReviewRequest implements Serializable {

    @NotNull(message = "Star rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5 stars")
    @Max(value = 5, message = "Rating must be between 1 and 5 stars")
    private Integer rating;

    @Size(max = 200, message = "Review title cannot exceed 200 characters")
    private String reviewTitle;

    @Size(max = 5000, message = "Review body cannot exceed 5000 characters")
    private String reviewBody;
}
