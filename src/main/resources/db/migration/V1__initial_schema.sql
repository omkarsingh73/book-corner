-- ============================================================================
-- V1__initial_schema.sql
-- Online Bookstore Platform ("Book Corner")
-- Complete Relational DDL Schema across all 7 Bounded Contexts:
--   1. member & store (Identity, RBAC, Store Tenancy)
--   2. catalog (Products, Categories, SKUs, Formats, Merchandising)
--   3. ordering (Shopping Carts, Orders, Line Items, RMAs, Coupons)
--   4. payment (Transactions, Multi-Tender Splits, Ledgers, Wallets)
--   5. shipping (Carrier Rate Cards, Consignments, Tracking Checkpoints)
--   6. review (Book Reviews, Ratings, Moderation, Helpful Votes)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. PostgreSQL Extensions
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ----------------------------------------------------------------------------
-- 2. Domain Schemas
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS member;
CREATE SCHEMA IF NOT EXISTS store;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS shipping;
CREATE SCHEMA IF NOT EXISTS review;

-- ============================================================================
-- MODULE 1: MEMBER & STORE
-- ============================================================================

CREATE TABLE member.users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    phone_number           VARCHAR(32) NULL,
    account_status         VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    last_login_at          TIMESTAMPTZ NULL,
    version                BIGINT NOT NULL DEFAULT 0,
    is_deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at             TIMESTAMPTZ NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'LOCKED')),
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX uq_users_email_active ON member.users (email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_account_status ON member.users (account_status) WHERE is_deleted = FALSE;

CREATE TABLE member.roles (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code    VARCHAR(64) NOT NULL,
    role_name    VARCHAR(128) NOT NULL,
    description  TEXT NULL,
    version      BIGINT NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_roles_code_active ON member.roles (role_code) WHERE is_deleted = FALSE;

CREATE TABLE member.user_roles (
    user_id      UUID NOT NULL,
    role_id      UUID NOT NULL,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES member.roles(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_user_roles_role ON member.user_roles (role_id);

CREATE TABLE member.user_entitlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    entitlement_key VARCHAR(64) NOT NULL,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ NULL,
    source_order_id UUID NULL,
    CONSTRAINT fk_user_entitlements_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uq_user_entitlements_key ON member.user_entitlements (user_id, entitlement_key);

CREATE TABLE member.user_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    address_type    VARCHAR(32) NOT NULL DEFAULT 'SHIPPING',
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    recipient_name  VARCHAR(150) NOT NULL,
    phone_number    VARCHAR(32) NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255) NULL,
    city            VARCHAR(100) NOT NULL,
    state_province  VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20) NOT NULL,
    country_code    VARCHAR(2) NOT NULL DEFAULT 'US',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_user_addresses_type CHECK (address_type IN ('SHIPPING', 'BILLING')),
    CONSTRAINT chk_user_addresses_country CHECK (length(country_code) = 2)
);

CREATE INDEX idx_user_addresses_user ON member.user_addresses (user_id) WHERE is_deleted = FALSE;

CREATE TABLE member.guest_sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_token VARCHAR(128) NOT NULL,
    store_id      UUID NULL,
    ip_address    VARCHAR(45) NULL,
    user_agent    VARCHAR(512) NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_guest_sessions_token ON member.guest_sessions (session_token);
CREATE INDEX idx_guest_sessions_expiry ON member.guest_sessions (expires_at);

CREATE TABLE member.audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_id   VARCHAR(64) NOT NULL,
    principal_type VARCHAR(32) NOT NULL,
    action         VARCHAR(64) NOT NULL,
    resource_type  VARCHAR(64) NOT NULL,
    resource_id    VARCHAR(64) NULL,
    ip_address     VARCHAR(45) NULL,
    user_agent     VARCHAR(512) NULL,
    old_values     JSONB NULL,
    new_values     JSONB NULL,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_resource ON member.audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_occurred ON member.audit_logs (occurred_at DESC);

CREATE TABLE member.wishlists (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    wishlist_name VARCHAR(100) NOT NULL DEFAULT 'Default Wishlist',
    is_public     BOOLEAN NOT NULL DEFAULT FALSE,
    share_token   VARCHAR(64) NULL,
    version       BIGINT NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uq_wishlists_user_name ON member.wishlists (user_id, wishlist_name) WHERE is_deleted = FALSE;

CREATE TABLE member.wishlist_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id    UUID NOT NULL,
    book_id        UUID NOT NULL,
    priority_level INT NOT NULL DEFAULT 3,
    note           VARCHAR(255) NULL,
    added_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES member.wishlists(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_wishlist_items_priority CHECK (priority_level BETWEEN 1 AND 5)
);

CREATE UNIQUE INDEX uq_wishlist_items_book ON member.wishlist_items (wishlist_id, book_id);

CREATE TABLE store.stores (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_code     VARCHAR(64) NOT NULL,
    store_name     VARCHAR(150) NOT NULL,
    currency_code  VARCHAR(3) NOT NULL DEFAULT 'USD',
    locale         VARCHAR(10) NOT NULL DEFAULT 'en_US',
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    version        BIGINT NOT NULL DEFAULT 0,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_stores_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_stores_code_active ON store.stores (store_code) WHERE is_deleted = FALSE;

CREATE TABLE store.store_policies (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id                     UUID NOT NULL,
    policy_type                  VARCHAR(64) NOT NULL,
    window_days                  INT NULL,
    grace_period_hours           INT NULL,
    min_order_free_shipping      BIGINT NULL,
    restocking_fee_basis_points  INT NOT NULL DEFAULT 0,
    policy_terms_text            TEXT NULL,
    version                      BIGINT NOT NULL DEFAULT 0,
    is_deleted                   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                   TIMESTAMPTZ NULL,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_store_policies_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_store_policies_type CHECK (policy_type IN ('RETURN', 'CANCELLATION', 'SHIPPING'))
);

CREATE UNIQUE INDEX uq_store_policies_unique ON store.store_policies (store_id, policy_type) WHERE is_deleted = FALSE;

CREATE TABLE store.store_catalogs (
    store_id      UUID NOT NULL,
    catalog_code  VARCHAR(64) NOT NULL,
    is_primary    BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_store_catalogs PRIMARY KEY (store_id, catalog_code),
    CONSTRAINT fk_store_catalogs_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================================
-- MODULE 2: CATALOG MASTER DATA
-- ============================================================================

CREATE TABLE catalog.publishers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publisher_name  VARCHAR(200) NOT NULL,
    publisher_code  VARCHAR(64) NOT NULL,
    contact_email   VARCHAR(255) NULL,
    website_url     VARCHAR(512) NULL,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_publishers_code_active ON catalog.publishers (publisher_code) WHERE is_deleted = FALSE;

CREATE TABLE catalog.authors (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(200) NOT NULL,
    author_slug  VARCHAR(200) NOT NULL,
    biography    TEXT NULL,
    avatar_url   VARCHAR(512) NULL,
    version      BIGINT NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_authors_slug_active ON catalog.authors (author_slug) WHERE is_deleted = FALSE;
CREATE INDEX idx_authors_name ON catalog.authors (full_name);

CREATE TABLE catalog.categories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_category_id  UUID NULL,
    category_name       VARCHAR(100) NOT NULL,
    category_slug       VARCHAR(120) NOT NULL,
    tree_level          INT NOT NULL DEFAULT 0,
    display_order       INT NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES catalog.categories(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_categories_tree_level CHECK (tree_level >= 0)
);

CREATE UNIQUE INDEX uq_categories_slug_parent ON catalog.categories (parent_category_id, category_slug) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_parent ON catalog.categories (parent_category_id) WHERE is_deleted = FALSE;

CREATE TABLE catalog.books (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn_13               VARCHAR(13) NOT NULL,
    isbn_10               VARCHAR(10) NULL,
    title                 VARCHAR(255) NOT NULL,
    subtitle              VARCHAR(255) NULL,
    publisher_id          UUID NOT NULL,
    primary_category_id   UUID NOT NULL,
    language              VARCHAR(10) NOT NULL DEFAULT 'en',
    publication_date      DATE NOT NULL,
    edition               VARCHAR(50) NULL,
    page_count            INT NULL,
    synopsis              TEXT NULL,
    cover_image_url       VARCHAR(512) NULL,
    entitlement_required  VARCHAR(64) NULL,
    version               BIGINT NOT NULL DEFAULT 0,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_books_category FOREIGN KEY (primary_category_id) REFERENCES catalog.categories(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_books_isbn13 CHECK (length(isbn_13) = 13),
    CONSTRAINT chk_books_page_count CHECK (page_count IS NULL OR page_count > 0)
);

CREATE UNIQUE INDEX uq_books_isbn13_active ON catalog.books (isbn_13) WHERE is_deleted = FALSE;
CREATE INDEX idx_books_category ON catalog.books (primary_category_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_books_title_trgm ON catalog.books USING gin (title gin_trgm_ops);

-- Link member.wishlist_items to catalog.books
ALTER TABLE member.wishlist_items 
    ADD CONSTRAINT fk_wishlist_items_book 
    FOREIGN KEY (book_id) REFERENCES catalog.books(id) 
    ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE TABLE catalog.book_authors (
    book_id            UUID NOT NULL,
    author_id          UUID NOT NULL,
    contribution_role  VARCHAR(50) NOT NULL DEFAULT 'AUTHOR',
    author_sequence    INT NOT NULL DEFAULT 1,
    CONSTRAINT pk_book_authors PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_book_authors_role CHECK (contribution_role IN ('AUTHOR', 'CO_AUTHOR', 'EDITOR', 'TRANSLATOR')),
    CONSTRAINT chk_book_authors_sequence CHECK (author_sequence >= 1)
);

CREATE INDEX idx_book_authors_author ON catalog.book_authors (author_id);

CREATE TABLE catalog.book_formats (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id            UUID NOT NULL,
    sku                VARCHAR(64) NOT NULL,
    format_type        VARCHAR(32) NOT NULL,
    base_price_amount  BIGINT NOT NULL,
    currency_code      VARCHAR(3) NOT NULL DEFAULT 'USD',
    weight_grams       INT NOT NULL DEFAULT 0,
    length_mm          INT NULL,
    width_mm           INT NULL,
    thickness_mm       INT NULL,
    stock_quantity     INT NOT NULL DEFAULT 0,
    version            BIGINT NOT NULL DEFAULT 0,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMPTZ NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_book_formats_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_book_formats_type CHECK (format_type IN ('PAPERBACK', 'HARDCOVER', 'EBOOK', 'AUDIOBOOK')),
    CONSTRAINT chk_book_formats_price CHECK (base_price_amount > 0),
    CONSTRAINT chk_book_formats_currency CHECK (length(currency_code) = 3),
    CONSTRAINT chk_book_formats_weight CHECK (weight_grams >= 0),
    CONSTRAINT chk_book_formats_stock CHECK (stock_quantity >= 0)
);

CREATE UNIQUE INDEX uq_book_formats_sku_active ON catalog.book_formats (sku) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uq_book_formats_book_type_active ON catalog.book_formats (book_id, format_type) WHERE is_deleted = FALSE;
CREATE INDEX idx_book_formats_book ON catalog.book_formats (book_id) WHERE is_deleted = FALSE;

CREATE TABLE catalog.merchandising_rules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_book_id       UUID NOT NULL,
    target_book_id       UUID NOT NULL,
    rule_type            VARCHAR(32) NOT NULL,
    priority_score       INT NOT NULL DEFAULT 100,
    discount_percentage  NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    version              BIGINT NOT NULL DEFAULT 0,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_merch_source_book FOREIGN KEY (source_book_id) REFERENCES catalog.books(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_merch_target_book FOREIGN KEY (target_book_id) REFERENCES catalog.books(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_merch_rule_type CHECK (rule_type IN ('UP_SELL', 'CROSS_SELL', 'BUNDLE')),
    CONSTRAINT chk_merch_discount CHECK (discount_percentage BETWEEN 0.00 AND 100.00)
);

CREATE UNIQUE INDEX uq_merch_rule_unique ON catalog.merchandising_rules (source_book_id, target_book_id, rule_type) WHERE is_deleted = FALSE;
CREATE INDEX idx_merch_source ON catalog.merchandising_rules (source_book_id) WHERE is_deleted = FALSE;

-- ============================================================================
-- MODULE 3: ORDERING & PROMOTIONS
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
    CONSTRAINT chk_coupons_dates CHECK (valid_to > valid_from)
);

CREATE UNIQUE INDEX uq_coupons_code_active ON ordering.coupons (coupon_code) WHERE is_deleted = FALSE;
CREATE INDEX idx_coupons_validity ON ordering.coupons (valid_from, valid_to) WHERE is_deleted = FALSE;

CREATE TABLE ordering.carts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NULL,
    guest_session_id  UUID NULL,
    store_id          UUID NOT NULL,
    coupon_code       VARCHAR(64) NULL,
    currency_code     VARCHAR(3) NOT NULL DEFAULT 'USD',
    expires_at        TIMESTAMPTZ NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_carts_guest FOREIGN KEY (guest_session_id) REFERENCES member.guest_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_carts_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_carts_owner CHECK (user_id IS NOT NULL OR guest_session_id IS NOT NULL),
    CONSTRAINT chk_carts_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_carts_user_active ON ordering.carts (user_id) WHERE user_id IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX uq_carts_guest_active ON ordering.carts (guest_session_id) WHERE guest_session_id IS NOT NULL AND is_deleted = FALSE;

CREATE TABLE ordering.cart_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id     UUID NOT NULL,
    format_id   UUID NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES ordering.carts(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_items_format FOREIGN KEY (format_id) REFERENCES catalog.book_formats(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 10)
);

CREATE UNIQUE INDEX uq_cart_items_unique ON ordering.cart_items (cart_id, format_id);

CREATE TABLE ordering.orders (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number               VARCHAR(64) NOT NULL,
    user_id                    UUID NOT NULL,
    store_id                   UUID NOT NULL,
    coupon_id                  UUID NULL,
    order_status               VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount            BIGINT NOT NULL,
    discount_amount            BIGINT NOT NULL DEFAULT 0,
    shipping_amount            BIGINT NOT NULL DEFAULT 0,
    tax_amount                 BIGINT NOT NULL DEFAULT 0,
    total_amount               BIGINT NOT NULL,
    currency_code              VARCHAR(3) NOT NULL DEFAULT 'USD',
    shipping_address_snapshot  JSONB NOT NULL,
    billing_address_snapshot   JSONB NOT NULL,
    placed_at                  TIMESTAMPTZ NULL,
    confirmed_at               TIMESTAMPTZ NULL,
    cancelled_at               TIMESTAMPTZ NULL,
    cancellation_reason        VARCHAR(255) NULL,
    version                    BIGINT NOT NULL DEFAULT 0,
    is_deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                 TIMESTAMPTZ NULL,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES ordering.coupons(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_orders_status CHECK (order_status IN (
        'DRAFT', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING',
        'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'
    )),
    CONSTRAINT chk_orders_total_calc CHECK (total_amount = subtotal_amount - discount_amount + shipping_amount + tax_amount)
);

CREATE UNIQUE INDEX uq_orders_number ON ordering.orders (order_number);
CREATE INDEX idx_orders_user_created ON ordering.orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status ON ordering.orders (order_status);

CREATE TABLE ordering.order_line_items (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id              UUID NOT NULL,
    format_id             UUID NOT NULL,
    book_title_snapshot   VARCHAR(255) NOT NULL,
    isbn_13_snapshot      VARCHAR(13) NOT NULL,
    format_type_snapshot  VARCHAR(32) NOT NULL,
    unit_price_amount     BIGINT NOT NULL,
    quantity              INT NOT NULL,
    line_discount_amount  BIGINT NOT NULL DEFAULT 0,
    line_tax_amount       BIGINT NOT NULL DEFAULT 0,
    line_total_amount     BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_line_items_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_line_items_format FOREIGN KEY (format_id) REFERENCES catalog.book_formats(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_line_items_qty CHECK (quantity >= 1)
);

CREATE INDEX idx_order_line_items_order ON ordering.order_line_items (order_id);

CREATE TABLE ordering.order_status_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL,
    from_status    VARCHAR(32) NULL,
    to_status      VARCHAR(32) NOT NULL,
    change_reason  VARCHAR(255) NULL,
    changed_by     VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_order_status_history_order ON ordering.order_status_history (order_id, changed_at ASC);

CREATE TABLE ordering.return_requests (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rma_number           VARCHAR(64) NOT NULL,
    order_id             UUID NOT NULL,
    user_id              UUID NOT NULL,
    return_status        VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    return_reason_code   VARCHAR(64) NOT NULL,
    customer_notes       TEXT NULL,
    rejection_reason     VARCHAR(255) NULL,
    refund_amount        BIGINT NOT NULL DEFAULT 0,
    version              BIGINT NOT NULL DEFAULT 0,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_return_requests_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uq_return_requests_rma ON ordering.return_requests (rma_number);

CREATE TABLE ordering.return_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_request_id  UUID NOT NULL,
    order_line_item_id UUID NOT NULL,
    quantity_returned  INT NOT NULL DEFAULT 1,
    item_condition     VARCHAR(32) NOT NULL DEFAULT 'UNOPENED',
    CONSTRAINT fk_return_items_request FOREIGN KEY (return_request_id) REFERENCES ordering.return_requests(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_return_items_line FOREIGN KEY (order_line_item_id) REFERENCES ordering.order_line_items(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================================
-- MODULE 4: PAYMENTS & WALLET
-- ============================================================================

CREATE TABLE payment.payment_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL,
    idempotency_key         VARCHAR(128) NOT NULL,
    transaction_status      VARCHAR(32) NOT NULL DEFAULT 'INITIALIZED',
    total_payable_amount    BIGINT NOT NULL,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'USD',
    gateway_provider        VARCHAR(64) NULL,
    gateway_transaction_id  VARCHAR(255) NULL,
    error_code              VARCHAR(64) NULL,
    error_message           TEXT NULL,
    authorized_at           TIMESTAMPTZ NULL,
    captured_at             TIMESTAMPTZ NULL,
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_payment_transactions_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_payment_tx_status CHECK (transaction_status IN ('INITIALIZED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'VOIDED', 'REFUNDED')),
    CONSTRAINT chk_payment_tx_amount CHECK (total_payable_amount > 0)
);

CREATE UNIQUE INDEX uq_payment_tx_idempotency ON payment.payment_transactions (idempotency_key);
CREATE INDEX idx_payment_tx_order ON payment.payment_transactions (order_id);

CREATE TABLE payment.tender_splits (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id  UUID NOT NULL,
    tender_type             VARCHAR(32) NOT NULL,
    amount                  BIGINT NOT NULL,
    tender_reference        VARCHAR(255) NULL,
    tender_status           VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED',
    CONSTRAINT fk_tender_splits_tx FOREIGN KEY (payment_transaction_id) REFERENCES payment.payment_transactions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_tender_type CHECK (tender_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'WALLET', 'GIFT_CARD', 'NET_BANKING')),
    CONSTRAINT chk_tender_amount CHECK (amount > 0)
);

CREATE TABLE payment.refunds (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id  UUID NOT NULL,
    refund_number           VARCHAR(64) NOT NULL,
    amount                  BIGINT NOT NULL,
    refund_status           VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reason                  VARCHAR(255) NOT NULL,
    gateway_refund_id       VARCHAR(255) NULL,
    processed_at            TIMESTAMPTZ NULL,
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_refunds_tx FOREIGN KEY (payment_transaction_id) REFERENCES payment.payment_transactions(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_refund_amount CHECK (amount > 0)
);

CREATE UNIQUE INDEX uq_refunds_number ON payment.refunds (refund_number);

CREATE TABLE payment.customer_wallets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    available_balance BIGINT NOT NULL DEFAULT 0,
    currency_code     VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    version           BIGINT NOT NULL DEFAULT 0,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_customer_wallets_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_wallet_balance CHECK (available_balance >= 0)
);

CREATE UNIQUE INDEX uq_customer_wallets_user ON payment.customer_wallets (user_id) WHERE is_deleted = FALSE;

CREATE TABLE payment.wallet_ledger (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id         UUID NOT NULL,
    entry_type        VARCHAR(16) NOT NULL,
    amount            BIGINT NOT NULL,
    running_balance   BIGINT NOT NULL,
    idempotency_key   VARCHAR(128) NOT NULL,
    reference_type    VARCHAR(32) NOT NULL,
    reference_id      VARCHAR(64) NOT NULL,
    narrative         VARCHAR(255) NULL,
    posted_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES payment.customer_wallets(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_running_balance CHECK (running_balance >= 0)
);

CREATE UNIQUE INDEX uq_wallet_ledger_idempotency ON payment.wallet_ledger (idempotency_key);
CREATE INDEX idx_wallet_ledger_wallet_posted ON payment.wallet_ledger (wallet_id, posted_at DESC);

CREATE TABLE payment.gift_vouchers (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_code       VARCHAR(64) NOT NULL,
    initial_balance    BIGINT NOT NULL,
    current_balance    BIGINT NOT NULL,
    currency_code      VARCHAR(3) NOT NULL DEFAULT 'USD',
    valid_to           TIMESTAMPTZ NOT NULL,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    version            BIGINT NOT NULL DEFAULT 0,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMPTZ NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_vouchers_balance CHECK (current_balance BETWEEN 0 AND initial_balance)
);

CREATE UNIQUE INDEX uq_gift_vouchers_code ON payment.gift_vouchers (voucher_code) WHERE is_deleted = FALSE;

-- ============================================================================
-- MODULE 5: SHIPPING & FULFILLMENT
-- ============================================================================

CREATE TABLE shipping.carrier_rate_cards (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_code                VARCHAR(64) NOT NULL,
    service_level               VARCHAR(64) NOT NULL,
    origin_zone                 VARCHAR(32) NOT NULL,
    destination_postal_prefix   VARCHAR(10) NOT NULL,
    max_weight_grams            INT NOT NULL,
    base_rate_amount            BIGINT NOT NULL,
    per_kg_rate_amount          BIGINT NOT NULL,
    estimated_transit_days_min  INT NOT NULL DEFAULT 1,
    estimated_transit_days_max  INT NOT NULL DEFAULT 5,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    version                     BIGINT NOT NULL DEFAULT 0,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_rate_cards_weight CHECK (max_weight_grams > 0),
    CONSTRAINT chk_rate_cards_base_rate CHECK (base_rate_amount >= 0),
    CONSTRAINT chk_rate_cards_per_kg CHECK (per_kg_rate_amount >= 0)
);

CREATE UNIQUE INDEX uq_carrier_rates_lookup 
    ON shipping.carrier_rate_cards (carrier_code, service_level, origin_zone, destination_postal_prefix, max_weight_grams) 
    WHERE is_deleted = FALSE;

CREATE TABLE shipping.shipping_consignments (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consignment_number         VARCHAR(64) NOT NULL,
    order_id                   UUID NOT NULL,
    carrier_code               VARCHAR(64) NOT NULL,
    service_level              VARCHAR(64) NOT NULL,
    tracking_number            VARCHAR(128) NOT NULL,
    consignment_status         VARCHAR(32) NOT NULL DEFAULT 'MANIFESTED',
    package_weight_grams       INT NOT NULL,
    shipping_charge_amount     BIGINT NOT NULL,
    destination_address_snap   JSONB NOT NULL,
    dispatched_at              TIMESTAMPTZ NULL,
    delivered_at               TIMESTAMPTZ NULL,
    version                    BIGINT NOT NULL DEFAULT 0,
    is_deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                 TIMESTAMPTZ NULL,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_consignments_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_consignments_status CHECK (consignment_status IN ('MANIFESTED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION'))
);

CREATE UNIQUE INDEX uq_consignments_number ON shipping.shipping_consignments (consignment_number);
CREATE UNIQUE INDEX uq_consignments_tracking ON shipping.shipping_consignments (tracking_number);
CREATE INDEX idx_consignments_order ON shipping.shipping_consignments (order_id);

CREATE TABLE shipping.consignment_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consignment_id          UUID NOT NULL,
    order_line_item_id      UUID NOT NULL,
    quantity_shipped        INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_consignment_items_consignment FOREIGN KEY (consignment_id) REFERENCES shipping.shipping_consignments(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_consignment_items_line FOREIGN KEY (order_line_item_id) REFERENCES ordering.order_line_items(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_consignment_items_qty CHECK (quantity_shipped >= 1)
);

CREATE TABLE shipping.tracking_events (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consignment_id       UUID NOT NULL,
    event_timestamp      TIMESTAMPTZ NOT NULL,
    checkpoint_location  VARCHAR(150) NULL,
    event_status         VARCHAR(64) NOT NULL,
    event_description    VARCHAR(255) NULL,
    raw_carrier_payload  JSONB NULL,
    recorded_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tracking_events_consignment FOREIGN KEY (consignment_id) REFERENCES shipping.shipping_consignments(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_tracking_events_consignment ON shipping.tracking_events (consignment_id, event_timestamp ASC);

-- ============================================================================
-- MODULE 6: REVIEWS & SOCIAL PROOF
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

CREATE UNIQUE INDEX uq_reviews_book_user_active ON review.reviews (book_id, user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_reviews_book_approved ON review.reviews (book_id, rating) INCLUDE (user_id, helpful_votes_count) WHERE moderation_status = 'APPROVED' AND is_deleted = FALSE;
CREATE INDEX idx_reviews_user ON review.reviews (user_id) WHERE is_deleted = FALSE;

CREATE TABLE review.review_helpful_votes (
    review_id   UUID NOT NULL,
    user_id     UUID NOT NULL,
    vote_type   VARCHAR(16) NOT NULL DEFAULT 'HELPFUL',
    voted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_review_helpful_votes PRIMARY KEY (review_id, user_id),
    CONSTRAINT fk_review_helpful_votes_review FOREIGN KEY (review_id) REFERENCES review.reviews(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_helpful_votes_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_review_vote_type CHECK (vote_type IN ('HELPFUL', 'UNHELPFUL'))
);

CREATE TABLE review.review_moderation_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id      UUID NOT NULL,
    moderator_id   UUID NOT NULL,
    action_taken   VARCHAR(32) NOT NULL,
    justification  TEXT NULL,
    actioned_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_moderation_review FOREIGN KEY (review_id) REFERENCES review.reviews(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_moderation_user FOREIGN KEY (moderator_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================================
-- INITIAL SEED: RBAC Roles & Default Tenancy Store
-- ============================================================================

INSERT INTO member.roles (id, role_code, role_name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN', 'Platform Administrator', 'Full platform backoffice access'),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_CUSTOMER', 'Registered Customer', 'Standard authenticated customer profile'),
    ('00000000-0000-0000-0000-000000000003', 'ROLE_GUEST', 'Anonymous Guest', 'Guest cart and checkout session')
ON CONFLICT DO NOTHING;

INSERT INTO store.stores (id, store_code, store_name, currency_code, locale, is_active) VALUES
    ('00000000-0000-0000-0000-000000000010', 'BK_MAIN_ONLINE', 'Book Corner Flagship Online', 'USD', 'en_US', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO store.store_policies (store_id, policy_type, window_days, grace_period_hours, min_order_free_shipping, restocking_fee_basis_points, policy_terms_text) VALUES
    ('00000000-0000-0000-0000-000000000010', 'RETURN', 30, 24, NULL, 0, 'Unopened physical books may be returned within 30 days of delivery.'),
    ('00000000-0000-0000-0000-000000000010', 'CANCELLATION', NULL, 1, NULL, 0, 'Orders may be cancelled within 1 hour of placement before fulfillment.'),
    ('00000000-0000-0000-0000-000000000010', 'SHIPPING', NULL, NULL, 3500, 0, 'Free standard ground shipping on qualifying orders exceeding $35.00.')
ON CONFLICT DO NOTHING;
