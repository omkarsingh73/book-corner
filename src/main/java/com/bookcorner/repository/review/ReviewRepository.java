package com.bookcorner.repository.review;

import com.bookcorner.entity.review.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ReviewEntity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID>, JpaSpecificationExecutor<ReviewEntity> {

    /**
     * Retrieve approved reviews for a book with customer user profile loaded.
     */
    @EntityGraph(attributePaths = {"user"})
    Page<ReviewEntity> findByBookIdAndModerationStatus(UUID bookId, String moderationStatus, Pageable pageable);

    /**
     * Check if a customer has already submitted a review for a specific book.
     */
    boolean existsByBookIdAndUserId(UUID bookId, UUID userId);

    /**
     * Calculate aggregate average rating and total review count for a book.
     */
    @Query("""
        SELECT COALESCE(AVG(r.ratingStars), 0.0), COUNT(r) 
        FROM ReviewEntity r 
        WHERE r.book.id = :bookId 
          AND r.moderationStatus = 'APPROVED'
        """)
    Object[] calculateRatingSummaryForBook(@Param("bookId") UUID bookId);

    /**
     * Atomically increment the helpful vote count of a review.
     */
    @Modifying
    @Query("""
        UPDATE ReviewEntity r 
        SET r.helpfulVotesCount = r.helpfulVotesCount + 1,
            r.version = r.version + 1
        WHERE r.id = :reviewId
        """)
    void incrementHelpfulVotes(@Param("reviewId") UUID reviewId);

    /**
     * Atomically increment the unhelpful vote count of a review.
     */
    @Modifying
    @Query("""
        UPDATE ReviewEntity r 
        SET r.unhelpfulVotesCount = r.unhelpfulVotesCount + 1,
            r.version = r.version + 1
        WHERE r.id = :reviewId
        """)
    void incrementUnhelpfulVotes(@Param("reviewId") UUID reviewId);
}
