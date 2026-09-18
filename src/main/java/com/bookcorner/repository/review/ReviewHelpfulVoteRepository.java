package com.bookcorner.repository.review;

import com.bookcorner.entity.review.ReviewHelpfulVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ReviewHelpfulVoteEntity.
 */
@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVoteEntity, UUID> {

    /**
     * Check if a customer has already voted on a review.
     */
    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

    /**
     * Find existing vote by review ID and user ID.
     */
    Optional<ReviewHelpfulVoteEntity> findByReviewIdAndUserId(UUID reviewId, UUID userId);
}
