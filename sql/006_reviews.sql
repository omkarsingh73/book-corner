-- ============================================================================
-- 006_reviews.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: review
-- Description: Customer ratings, written book reviews, moderation workflows,
--              verified purchase indicators, and helpfulness up/down votes.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS review;

-- ============================================================================
-- TABLE: review.reviews
-- ============================================================================
CREATE TABLE review.reviews (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id               UUID NOT NULL,
    user_id               UUID NOT NULL,
    rating                INT NOT NULL,
    review_title          VARCHAR(200) NULL,
    review_body           TEXT NULL,
    is_verified_purchase  BOOLEAN NOT NULL DEFAULT FALSE,
    moderation_status     VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    helpful_votes_count   INT NOT NULL DEFAULT 0,
    
    -- Audit & Concurrency Columns
    version               BIGINT NOT NULL DEFAULT 0,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_moderation CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_reviews_helpful_votes CHECK (helpful_votes_count >= 0)
);

CREATE UNIQUE INDEX uq_reviews_book_user_active 
    ON review.reviews (book_id, user_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_reviews_book_approved 
    ON review.reviews (book_id, rating) 
    INCLUDE (user_id, helpful_votes_count) 
    WHERE moderation_status = 'APPROVED' AND is_deleted = FALSE;

CREATE INDEX idx_reviews_user 
    ON review.reviews (user_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: review.review_helpful_votes
-- ============================================================================
CREATE TABLE review.review_helpful_votes (
    review_id   UUID NOT NULL,
    user_id     UUID NOT NULL,
    is_helpful  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_review_helpful_votes PRIMARY KEY (review_id, user_id),
    CONSTRAINT fk_helpful_votes_review FOREIGN KEY (review_id) REFERENCES review.reviews(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_helpful_votes_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_helpful_votes_user ON review.review_helpful_votes (user_id);
