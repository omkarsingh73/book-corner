-- ============================================================================
-- 007_coupons.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: ordering
-- Description: Promotional coupons, discount types, validity ranges, usage limits,
--              and deferred foreign key resolution for ordering.orders.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ordering;

-- ============================================================================
-- TABLE: ordering.coupons
-- ============================================================================
CREATE TABLE ordering.coupons (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_code           VARCHAR(64) NOT NULL,
    discount_type         VARCHAR(32) NOT NULL,
    discount_value        BIGINT NOT NULL,
    min_order_amount      BIGINT NOT NULL DEFAULT 0,
    max_discount_amount   BIGINT NULL,
    valid_from            TIMESTAMPTZ NOT NULL,
    valid_to              TIMESTAMPTZ NOT NULL,
    usage_limit_total     INT NULL,
    usage_limit_per_user  INT NOT NULL DEFAULT 1,
    current_usage_count   INT NOT NULL DEFAULT 0,
    
    -- Audit & Concurrency Columns
    version               BIGINT NOT NULL DEFAULT 0,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT chk_coupons_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_coupons_value CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_min_order CHECK (min_order_amount >= 0),
    CONSTRAINT chk_coupons_max_discount CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT chk_coupons_dates CHECK (valid_to > valid_from),
    CONSTRAINT chk_coupons_usage_total CHECK (usage_limit_total IS NULL OR usage_limit_total > 0),
    CONSTRAINT chk_coupons_usage_user CHECK (usage_limit_per_user >= 1),
    CONSTRAINT chk_coupons_current_usage CHECK (current_usage_count >= 0)
);

CREATE UNIQUE INDEX uq_coupons_code_active 
    ON ordering.coupons (coupon_code) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_coupons_validity 
    ON ordering.coupons (valid_from, valid_to) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- DEFERRED FOREIGN KEY: Link ordering.orders to ordering.coupons
-- ============================================================================
ALTER TABLE ordering.orders 
    ADD CONSTRAINT fk_orders_coupon 
    FOREIGN KEY (coupon_id) REFERENCES ordering.coupons(id) 
    ON DELETE SET NULL ON UPDATE CASCADE;

CREATE INDEX idx_orders_coupon ON ordering.orders (coupon_id);
