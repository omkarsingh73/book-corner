-- ============================================================================
-- 002_catalog.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: catalog
-- Description: Product master data for publishers, authors, hierarchical categories,
--              books, author associations, format SKUs, and merchandising rules.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS catalog;

-- ============================================================================
-- TABLE: catalog.publishers
-- ============================================================================
CREATE TABLE catalog.publishers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publisher_name  VARCHAR(200) NOT NULL,
    publisher_code  VARCHAR(64) NOT NULL,
    contact_email   VARCHAR(255) NULL,
    website_url     VARCHAR(512) NULL,
    
    -- Audit & Concurrency Columns
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_publishers_code_active 
    ON catalog.publishers (publisher_code) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: catalog.authors
-- ============================================================================
CREATE TABLE catalog.authors (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(200) NOT NULL,
    author_slug  VARCHAR(200) NOT NULL,
    biography    TEXT NULL,
    avatar_url   VARCHAR(512) NULL,
    
    -- Audit & Concurrency Columns
    version      BIGINT NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uq_authors_slug_active 
    ON catalog.authors (author_slug) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_authors_name ON catalog.authors (full_name);


-- ============================================================================
-- TABLE: catalog.categories (Self-referential hierarchy)
-- ============================================================================
CREATE TABLE catalog.categories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_category_id  UUID NULL,
    category_name       VARCHAR(100) NOT NULL,
    category_slug       VARCHAR(120) NOT NULL,
    tree_level          INT NOT NULL DEFAULT 0,
    display_order       INT NOT NULL DEFAULT 0,
    
    -- Audit & Concurrency Columns
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

CREATE UNIQUE INDEX uq_categories_slug_parent 
    ON catalog.categories (parent_category_id, category_slug) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_categories_parent 
    ON catalog.categories (parent_category_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: catalog.books
-- ============================================================================
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
    
    -- Audit & Concurrency Columns
    version               BIGINT NOT NULL DEFAULT 0,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES catalog.publishers(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_books_category FOREIGN KEY (primary_category_id) REFERENCES catalog.categories(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_books_isbn13 CHECK (length(isbn_13) = 13),
    CONSTRAINT chk_books_page_count CHECK (page_count IS NULL OR page_count > 0)
);

CREATE UNIQUE INDEX uq_books_isbn13_active 
    ON catalog.books (isbn_13) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_books_category 
    ON catalog.books (primary_category_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_books_publisher 
    ON catalog.books (publisher_id) 
    WHERE is_deleted = FALSE;

-- GIN Trigram index for typo-tolerant fuzzy title search
CREATE INDEX idx_books_title_trgm 
    ON catalog.books USING gin (title gin_trgm_ops);


-- ============================================================================
-- TABLE: catalog.book_authors (M:N Junction)
-- ============================================================================
CREATE TABLE catalog.book_authors (
    book_id            UUID NOT NULL,
    author_id          UUID NOT NULL,
    contribution_role  VARCHAR(50) NOT NULL DEFAULT 'AUTHOR',
    author_sequence    INT NOT NULL DEFAULT 1,
    
    CONSTRAINT pk_book_authors PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES catalog.authors(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_book_authors_role CHECK (contribution_role IN ('AUTHOR', 'CO_AUTHOR', 'EDITOR', 'TRANSLATOR')),
    CONSTRAINT chk_book_authors_sequence CHECK (author_sequence >= 1)
);

CREATE INDEX idx_book_authors_author ON catalog.book_authors (author_id);


-- ============================================================================
-- TABLE: catalog.book_formats (SKU Level)
-- ============================================================================
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
    
    -- Audit & Concurrency Columns
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

CREATE UNIQUE INDEX uq_book_formats_sku_active 
    ON catalog.book_formats (sku) 
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_book_formats_book_type_active 
    ON catalog.book_formats (book_id, format_type) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_book_formats_book 
    ON catalog.book_formats (book_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: catalog.merchandising_rules (Up-Sell & Cross-Sell)
-- ============================================================================
CREATE TABLE catalog.merchandising_rules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_book_id       UUID NOT NULL,
    target_book_id       UUID NOT NULL,
    rule_type            VARCHAR(32) NOT NULL,
    priority_score       INT NOT NULL DEFAULT 100,
    discount_percentage  NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    
    -- Audit & Concurrency Columns
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

CREATE UNIQUE INDEX uq_merch_rule_unique 
    ON catalog.merchandising_rules (source_book_id, target_book_id, rule_type) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_merch_source 
    ON catalog.merchandising_rules (source_book_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- DEFERRED FOREIGN KEYS: Link member.wishlist_items to catalog.books
-- ============================================================================
ALTER TABLE member.wishlist_items 
    ADD CONSTRAINT fk_wishlist_items_book 
    FOREIGN KEY (book_id) REFERENCES catalog.books(id) 
    ON DELETE RESTRICT ON UPDATE CASCADE;
