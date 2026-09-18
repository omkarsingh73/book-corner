-- ============================================================================
-- V6__align_schema_with_entities.sql
-- Online Bookstore Platform ("Book Corner")
-- Adds surrogate keys and audit columns to book_authors and review_helpful_votes
-- ============================================================================

-- 1. catalog.book_authors: Add surrogate key, audit fields, and uniqueness constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'catalog' AND table_name = 'book_authors' AND column_name = 'id'
    ) THEN
        ALTER TABLE catalog.book_authors ADD COLUMN id UUID DEFAULT gen_random_uuid();
        UPDATE catalog.book_authors SET id = gen_random_uuid() WHERE id IS NULL;
        ALTER TABLE catalog.book_authors ALTER COLUMN id SET NOT NULL;
        ALTER TABLE catalog.book_authors DROP CONSTRAINT IF EXISTS pk_book_authors CASCADE;
        ALTER TABLE catalog.book_authors ADD CONSTRAINT pk_book_authors PRIMARY KEY (id);
        ALTER TABLE catalog.book_authors ADD CONSTRAINT uq_book_authors_book_author UNIQUE (book_id, author_id);
    END IF;
END $$;

ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS created_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE catalog.book_authors ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';

-- 2. review.review_helpful_votes: Add surrogate key, audit fields, and is_helpful flag
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'review' AND table_name = 'review_helpful_votes' AND column_name = 'id'
    ) THEN
        ALTER TABLE review.review_helpful_votes ADD COLUMN id UUID DEFAULT gen_random_uuid();
        UPDATE review.review_helpful_votes SET id = gen_random_uuid() WHERE id IS NULL;
        ALTER TABLE review.review_helpful_votes ALTER COLUMN id SET NOT NULL;
        ALTER TABLE review.review_helpful_votes DROP CONSTRAINT IF EXISTS pk_review_helpful_votes CASCADE;
        ALTER TABLE review.review_helpful_votes ADD CONSTRAINT pk_review_helpful_votes PRIMARY KEY (id);
        ALTER TABLE review.review_helpful_votes ADD CONSTRAINT uq_review_helpful_votes_review_user UNIQUE (review_id, user_id);
    END IF;
END $$;

ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS is_helpful BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS created_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE review.review_helpful_votes ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';

-- 3. store.store_policies: Add configured policy thresholds
ALTER TABLE store.store_policies ADD COLUMN IF NOT EXISTS cancellation_grace_minutes INT NOT NULL DEFAULT 60;
ALTER TABLE store.store_policies ADD COLUMN IF NOT EXISTS return_window_days INT NOT NULL DEFAULT 30;
ALTER TABLE store.store_policies ADD COLUMN IF NOT EXISTS free_shipping_threshold_amount BIGINT NOT NULL DEFAULT 5000;

-- Consolidate multiple policy rows per store into 1 row matching the @OneToOne StorePolicyEntity
DELETE FROM store.store_policies WHERE store_id = '00000000-0000-0000-0000-000000000010' AND policy_type != 'SHIPPING';
UPDATE store.store_policies SET 
    cancellation_grace_minutes = 60,
    return_window_days = 30,
    free_shipping_threshold_amount = 3500
WHERE store_id = '00000000-0000-0000-0000-000000000010';
