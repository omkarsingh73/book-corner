package com.bookcorner.entity.review;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.member.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a customer book review and rating.
 * Maps to review.reviews table.
 */
@Entity
@Table(name = "reviews", schema = "review")
@SQLDelete(sql = "UPDATE review.reviews SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReviewEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "rating_stars", nullable = false)
    private int ratingStars;

    @Column(name = "review_title", length = 200)
    private String reviewTitle;

    @Column(name = "review_body", columnDefinition = "TEXT")
    private String reviewBody;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private boolean isVerifiedPurchase = false;

    @Column(name = "helpful_votes_count", nullable = false)
    @Builder.Default
    private int helpfulVotesCount = 0;

    @Column(name = "unhelpful_votes_count", nullable = false)
    @Builder.Default
    private int unhelpfulVotesCount = 0;

    @Column(name = "moderation_status", nullable = false, length = 32)
    @Builder.Default
    private String moderationStatus = "APPROVED";

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewHelpfulVoteEntity> votes = new ArrayList<>();
}
