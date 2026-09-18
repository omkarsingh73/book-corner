package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.PageMetaDto;
import com.bookcorner.dto.review.BookReviewsResponse;
import com.bookcorner.dto.review.ReviewDto;
import com.bookcorner.dto.review.SubmitReviewRequest;
import com.bookcorner.dto.review.VoteReviewRequest;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.review.ReviewEntity;
import com.bookcorner.entity.review.ReviewHelpfulVoteEntity;
import com.bookcorner.mapper.ReviewMapper;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.review.ReviewHelpfulVoteRepository;
import com.bookcorner.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Customer Ratings and Book Reviews Service.
 * Manages verified purchase review submissions, community moderation states,
 * aggregate star calculations, and helpfulness voting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Retrieves paginated approved customer reviews with summary rating metrics.
     */
    @Transactional(readOnly = true)
    public BookReviewsResponse listBookReviews(UUID bookId, Pageable pageable) {
        log.info("Fetching approved reviews for book ID: {}, page: {}", bookId, pageable.getPageNumber());

        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with ID: " + bookId);
        }

        Page<ReviewEntity> reviewPage = reviewRepository.findByBookIdAndModerationStatus(bookId, "APPROVED", pageable);

        Double avgRating = 0.0;
        Integer totalReviews = 0;

        try {
            Object[] stats = reviewRepository.calculateRatingSummaryForBook(bookId);
            if (stats != null && stats.length > 0) {
                Object item = stats[0];
                if (item instanceof Object[] row) {
                    avgRating = ((Number) row[0]).doubleValue();
                    totalReviews = ((Number) row[1]).intValue();
                } else if (item instanceof Number avgNum && stats.length > 1) {
                    avgRating = avgNum.doubleValue();
                    totalReviews = ((Number) stats[1]).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate aggregate rating metrics for book {}: {}", bookId, e.getMessage());
        }

        return BookReviewsResponse.builder()
                .bookId(bookId)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .data(reviewMapper.toReviewDtoList(reviewPage.getContent()))
                .pagination(PageMetaDto.fromPage(reviewPage))
                .build();
    }

    /**
     * Submits a customer review and rating, determining verified purchase status.
     */
    @Transactional
    public ReviewDto submitReview(UUID userId, UUID bookId, SubmitReviewRequest request) {
        log.info("Submitting review for book ID: {} by user ID: {}", bookId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            log.warn("Duplicate review attempt by user {} for book {}", userId, bookId);
            throw new DuplicateResourceException("You have already submitted a review for this book.");
        }

        // Check if customer completed a purchase containing this book
        boolean isVerified = orderRepository.hasUserPurchasedBook(userId, bookId);
        log.info("Review for book {} by user {}: isVerifiedPurchase={}", bookId, userId, isVerified);

        ReviewEntity review = ReviewEntity.builder()
                .book(book)
                .user(user)
                .ratingStars(request.getRating())
                .reviewTitle(request.getReviewTitle())
                .reviewBody(request.getReviewBody())
                .isVerifiedPurchase(isVerified)
                .moderationStatus("APPROVED")
                .helpfulVotesCount(0)
                .unhelpfulVotesCount(0)
                .build();

        ReviewEntity saved = reviewRepository.save(review);
        log.info("Review successfully created with ID: {}", saved.getId());

        return reviewMapper.toReviewDto(saved);
    }

    /**
     * Casts or toggles a helpfulness community vote on a review.
     */
    @Transactional
    public void voteReview(UUID userId, UUID reviewId, VoteReviewRequest request) {
        log.info("User {} casting vote (isHelpful={}) on review {}", userId, request.getIsHelpful(), reviewId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (review.getUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("Authors cannot vote on their own reviews.");
        }

        Optional<ReviewHelpfulVoteEntity> existingVoteOpt = reviewHelpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId);

        if (existingVoteOpt.isPresent()) {
            ReviewHelpfulVoteEntity existingVote = existingVoteOpt.get();
            if (existingVote.isHelpful() == request.getIsHelpful()) {
                throw new DuplicateResourceException("You have already cast this vote on this review.");
            }

            // Flip the vote
            existingVote.setHelpful(request.getIsHelpful());
            reviewHelpfulVoteRepository.save(existingVote);

            if (request.getIsHelpful()) {
                review.setHelpfulVotesCount(review.getHelpfulVotesCount() + 1);
                review.setUnhelpfulVotesCount(Math.max(0, review.getUnhelpfulVotesCount() - 1));
            } else {
                review.setUnhelpfulVotesCount(review.getUnhelpfulVotesCount() + 1);
                review.setHelpfulVotesCount(Math.max(0, review.getHelpfulVotesCount() - 1));
            }
            reviewRepository.save(review);
            log.info("User {} flipped vote on review {}", userId, reviewId);
        } else {
            ReviewHelpfulVoteEntity newVote = ReviewHelpfulVoteEntity.builder()
                    .review(review)
                    .user(user)
                    .isHelpful(request.getIsHelpful())
                    .build();
            reviewHelpfulVoteRepository.save(newVote);

            if (request.getIsHelpful()) {
                reviewRepository.incrementHelpfulVotes(reviewId);
            } else {
                reviewRepository.incrementUnhelpfulVotes(reviewId);
            }
            log.info("New vote recorded for review {}", reviewId);
        }
    }
}
