-- ============================================================================
-- 001_users.sql
-- Online Bookstore Platform ("Book Corner")
-- Schemas: member, store
-- Description: Core identity, roles, entitlements, address book, guest sessions,
--              wishlists, and multi-store configuration with store policies.
-- ============================================================================

-- Enable Required PostgreSQL Extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create Domain Schemas
CREATE SCHEMA IF NOT EXISTS member;
CREATE SCHEMA IF NOT EXISTS store;

-- ============================================================================
-- TABLE: member.users
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
    
    -- Audit & Concurrency Columns
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

-- Soft-delete partial unique index on email
CREATE UNIQUE INDEX uq_users_email_active 
    ON member.users (email) 
    WHERE is_deleted = FALSE;

-- Performance index on account status
CREATE INDEX idx_users_account_status 
    ON member.users (account_status) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: member.roles
-- ============================================================================
CREATE TABLE member.roles (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code    VARCHAR(64) NOT NULL,
    role_name    VARCHAR(128) NOT NULL,
    description  TEXT NULL,
    
    -- Audit & Concurrency Columns
    version      BIGINT NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_roles_code_active 
    ON member.roles (role_code) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: member.user_roles (M:N Junction)
-- ============================================================================
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


-- ============================================================================
-- TABLE: member.user_entitlements
-- ============================================================================
CREATE TABLE member.user_entitlements (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    entitlement_code  VARCHAR(64) NOT NULL,
    valid_from        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to          TIMESTAMPTZ NULL,
    
    -- Audit & Concurrency Columns
    version           BIGINT NOT NULL DEFAULT 0,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_user_entitlements_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_entitlement_dates CHECK (valid_to IS NULL OR valid_to > valid_from)
);

CREATE UNIQUE INDEX uq_user_entitlements_active 
    ON member.user_entitlements (user_id, entitlement_code) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_user_entitlements_user 
    ON member.user_entitlements (user_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: member.user_addresses
-- ============================================================================
CREATE TABLE member.user_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    address_type    VARCHAR(32) NOT NULL DEFAULT 'SHIPPING',
    recipient_name  VARCHAR(150) NOT NULL,
    phone_number    VARCHAR(32) NOT NULL,
    street_line1    VARCHAR(255) NOT NULL,
    street_line2    VARCHAR(255) NULL,
    city            VARCHAR(100) NOT NULL,
    state_province  VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20) NOT NULL,
    country_code    VARCHAR(2) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Audit & Concurrency Columns
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_addresses_type CHECK (address_type IN ('SHIPPING', 'BILLING', 'BOTH')),
    CONSTRAINT chk_addresses_country CHECK (length(country_code) = 2)
);

CREATE INDEX idx_user_addresses_user 
    ON member.user_addresses (user_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_user_addresses_default 
    ON member.user_addresses (user_id, is_default) 
    WHERE is_default = TRUE AND is_deleted = FALSE;


-- ============================================================================
-- TABLE: member.guest_sessions
-- ============================================================================
CREATE TABLE member.guest_sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_token      VARCHAR(128) NOT NULL,
    ip_address         VARCHAR(45) NULL,
    user_agent         TEXT NULL,
    expires_at         TIMESTAMPTZ NOT NULL,
    converted_user_id  UUID NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_guest_sessions_converted_user FOREIGN KEY (converted_user_id) REFERENCES member.users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uq_guest_sessions_token ON member.guest_sessions (session_token);
CREATE INDEX idx_guest_sessions_expires ON member.guest_sessions (expires_at);


-- ============================================================================
-- TABLE: member.wishlists
-- ============================================================================
CREATE TABLE member.wishlists (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    wishlist_name VARCHAR(100) NOT NULL DEFAULT 'My Wishlist',
    is_public     BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Audit & Concurrency Columns
    version       BIGINT NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uq_wishlists_user_name 
    ON member.wishlists (user_id, wishlist_name) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: member.wishlist_items
-- Note: Foreign key to catalog.books(id) is established in 002_catalog.sql.
-- ============================================================================
CREATE TABLE member.wishlist_items (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id          UUID NOT NULL,
    book_id              UUID NOT NULL,
    desired_price_alert  BIGINT NULL,
    added_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES member.wishlists(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_wishlist_items_price_alert CHECK (desired_price_alert IS NULL OR desired_price_alert > 0)
);

CREATE UNIQUE INDEX uq_wishlist_items_unique ON member.wishlist_items (wishlist_id, book_id);
CREATE INDEX idx_wishlist_items_book ON member.wishlist_items (book_id);


-- ============================================================================
-- TABLE: store.stores
-- ============================================================================
CREATE TABLE store.stores (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_code     VARCHAR(64) NOT NULL,
    store_name     VARCHAR(150) NOT NULL,
    currency_code  VARCHAR(3) NOT NULL DEFAULT 'USD',
    locale         VARCHAR(16) NOT NULL DEFAULT 'en_US',
    timezone       VARCHAR(64) NOT NULL DEFAULT 'UTC',
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit & Concurrency Columns
    version        BIGINT NOT NULL DEFAULT 0,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT chk_stores_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_stores_code_active 
    ON store.stores (store_code) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: store.store_policies
-- ============================================================================
CREATE TABLE store.store_policies (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id                     UUID NOT NULL,
    policy_type                  VARCHAR(64) NOT NULL,
    window_days                  INT NULL,
    grace_period_hours           INT NULL,
    min_order_free_shipping      BIGINT NULL,
    restocking_fee_basis_points  INT NOT NULL DEFAULT 0,
    policy_terms_text            TEXT NULL,
    
    -- Audit & Concurrency Columns
    version                      BIGINT NOT NULL DEFAULT 0,
    is_deleted                   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                   TIMESTAMPTZ NULL,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_store_policies_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_store_policies_type CHECK (policy_type IN ('RETURN', 'CANCELLATION', 'SHIPPING')),
    CONSTRAINT chk_store_policies_window CHECK (window_days IS NULL OR window_days >= 0),
    CONSTRAINT chk_store_policies_grace CHECK (grace_period_hours IS NULL OR grace_period_hours >= 0),
    CONSTRAINT chk_store_policies_free_ship CHECK (min_order_free_shipping IS NULL OR min_order_free_shipping >= 0),
    CONSTRAINT chk_store_policies_restock_fee CHECK (restocking_fee_basis_points BETWEEN 0 AND 10000)
);

CREATE UNIQUE INDEX uq_store_policies_unique 
    ON store.store_policies (store_id, policy_type) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: store.store_catalogs
-- ============================================================================
CREATE TABLE store.store_catalogs (
    store_id      UUID NOT NULL,
    catalog_code  VARCHAR(64) NOT NULL,
    is_primary    BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_store_catalogs PRIMARY KEY (store_id, catalog_code),
    CONSTRAINT fk_store_catalogs_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE CASCADE ON UPDATE CASCADE
);
