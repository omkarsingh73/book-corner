package com.bookcorner.controller;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.review.BookReviewsResponse;
import com.bookcorner.dto.review.ReviewDto;
import com.bookcorner.dto.review.SubmitReviewRequest;
import com.bookcorner.dto.review.VoteReviewRequest;
import com.bookcorner.dto.review.VoteReviewResponse;
import com.bookcorner.repository.review.ReviewRepository;
import com.bookcorner.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer Reviews, Ratings, and Helpfulness Voting REST Controller.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Customer ratings (1-5 stars), verified purchase written reviews, and helpfulness voting.")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    @Operation(summary = "List customer book reviews", operationId = "listBookReviews")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book review listing retrieved."),
            @ApiResponse(responseCode = "404", description = "Book not found.")
    })
    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<BookReviewsResponse> listBookReviews(
            @Parameter(description = "Book ID", required = true)
            @PathVariable("bookId") UUID bookId,
            @Parameter(description = "Sorting criteria (HELPFUL, RECENT, RATING_HIGH, RATING_LOW)")
            @RequestParam(value = "sort", defaultValue = "HELPFUL") String sort,
            @PageableDefault(page = 0, size = 10, sort = "helpfulVotesCount", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /books/{}/reviews: sort={}, page={}", bookId, sort, pageable.getPageNumber());
        BookReviewsResponse reviews = reviewService.listBookReviews(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Submit a verified review and rating", operationId = "submitBookReview",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review accepted successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Rating out of bounds."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Book not found."),
            @ApiResponse(responseCode = "409", description = "Duplicate - Customer already reviewed this book.")
    })
    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewDto> submitBookReview(
            Authentication authentication,
            @Parameter(description = "Book ID", required = true)
            @PathVariable("bookId") UUID bookId,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /books/{}/reviews for user: {}, rating: {}", bookId, userId, request.getRating());
        ReviewDto created = reviewService.submitReview(userId, bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Vote on review helpfulness", operationId = "voteReviewHelpful",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vote recorded."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Cannot vote on own review."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Review not found.")
    })
    @PostMapping("/reviews/{reviewId}/votes")
    public ResponseEntity<VoteReviewResponse> voteReviewHelpful(
            Authentication authentication,
            @Parameter(description = "Review ID", required = true)
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody VoteReviewRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /reviews/{}/votes: isHelpful={}", reviewId, request.getIsHelpful());
        reviewService.voteReview(userId, reviewId, request);

        int helpfulVotes = reviewRepository.findById(reviewId)
                .map(r -> r.getHelpfulVotesCount())
                .orElse(1);

        VoteReviewResponse response = VoteReviewResponse.builder()
                .reviewId(reviewId)
                .helpfulVotesCount(helpfulVotes)
                .userVote(request.getIsHelpful())
                .build();

        return ResponseEntity.ok(response);
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedOperationException("Authentication required to interact with reviews.");
        }
        if (authentication.getPrincipal() instanceof com.bookcorner.security.UserPrincipal principal) {
            return principal.getId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
    }
}
