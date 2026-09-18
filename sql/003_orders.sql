-- ============================================================================
-- 003_orders.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: ordering
-- Description: Shopping carts, cart items, order aggregates, immutable order line items,
--              status transition audit history, and return requests (RMA).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ordering;

-- ============================================================================
-- TABLE: ordering.carts
-- ============================================================================
CREATE TABLE ordering.carts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NULL,
    guest_session_id  UUID NULL,
    store_id          UUID NOT NULL,
    coupon_code       VARCHAR(64) NULL,
    currency_code     VARCHAR(3) NOT NULL DEFAULT 'USD',
    expires_at        TIMESTAMPTZ NOT NULL,
    
    -- Audit & Concurrency Columns
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

CREATE UNIQUE INDEX uq_carts_user_active 
    ON ordering.carts (user_id) 
    WHERE user_id IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX uq_carts_guest_active 
    ON ordering.carts (guest_session_id) 
    WHERE guest_session_id IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_carts_lookup_active 
    ON ordering.carts (user_id, store_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: ordering.cart_items
-- ============================================================================
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
CREATE INDEX idx_cart_items_format ON ordering.cart_items (format_id);


-- ============================================================================
-- TABLE: ordering.orders
-- Note: Foreign key to ordering.coupons(id) is established in 007_coupons.sql.
-- ============================================================================
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
    
    -- Audit & Concurrency Columns
    version                    BIGINT NOT NULL DEFAULT 0,
    is_deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                 TIMESTAMPTZ NULL,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES store.stores(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_orders_status CHECK (order_status IN (
        'DRAFT', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING',
        'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'
    )),
    CONSTRAINT chk_orders_subtotal CHECK (subtotal_amount >= 0),
    CONSTRAINT chk_orders_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_orders_shipping CHECK (shipping_amount >= 0),
    CONSTRAINT chk_orders_tax CHECK (tax_amount >= 0),
    CONSTRAINT chk_orders_total_calc CHECK (total_amount = subtotal_amount - discount_amount + shipping_amount + tax_amount),
    CONSTRAINT chk_orders_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_orders_number ON ordering.orders (order_number);

-- Composite covering index for customer order history dashboard
CREATE INDEX idx_orders_user_created 
    ON ordering.orders (user_id, created_at DESC) 
    INCLUDE (order_number, total_amount, order_status);

CREATE INDEX idx_orders_status ON ordering.orders (order_status);


-- ============================================================================
-- TABLE: ordering.order_line_items
-- ============================================================================
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
    CONSTRAINT chk_line_items_price CHECK (unit_price_amount > 0),
    CONSTRAINT chk_line_items_qty CHECK (quantity >= 1),
    CONSTRAINT chk_line_items_discount CHECK (line_discount_amount >= 0),
    CONSTRAINT chk_line_items_tax CHECK (line_tax_amount >= 0),
    CONSTRAINT chk_line_items_total CHECK (line_total_amount = (unit_price_amount * quantity) - line_discount_amount + line_tax_amount)
);

CREATE INDEX idx_order_line_items_order ON ordering.order_line_items (order_id);
CREATE INDEX idx_order_line_items_format ON ordering.order_line_items (format_id);


-- ============================================================================
-- TABLE: ordering.order_status_history
-- ============================================================================
CREATE TABLE ordering.order_status_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL,
    from_status  VARCHAR(32) NULL,
    to_status    VARCHAR(32) NOT NULL,
    remarks      VARCHAR(512) NULL,
    changed_by   VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_order_status_history_order ON ordering.order_status_history (order_id, changed_at);


-- ============================================================================
-- TABLE: ordering.return_requests (RMA)
-- ============================================================================
CREATE TABLE ordering.return_requests (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rma_number              VARCHAR(64) NOT NULL,
    order_id                UUID NOT NULL,
    user_id                 UUID NOT NULL,
    rma_status              VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    return_reason_code      VARCHAR(64) NOT NULL,
    customer_remarks        TEXT NULL,
    inspection_notes        TEXT NULL,
    return_tracking_number  VARCHAR(100) NULL,
    restocking_fee_amount   BIGINT NOT NULL DEFAULT 0,
    refund_amount           BIGINT NOT NULL DEFAULT 0,
    
    -- Audit & Concurrency Columns
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_return_requests_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_rma_status CHECK (rma_status IN (
        'REQUESTED', 'APPROVED', 'SHIPPED_BACK', 
        'INSPECTED_ACCEPTED', 'INSPECTED_REJECTED', 'REFUNDED'
    )),
    CONSTRAINT chk_rma_restock_fee CHECK (restocking_fee_amount >= 0),
    CONSTRAINT chk_rma_refund_amount CHECK (refund_amount >= 0)
);

CREATE UNIQUE INDEX uq_returns_rma_number ON ordering.return_requests (rma_number);
CREATE INDEX idx_returns_order ON ordering.return_requests (order_id);
CREATE INDEX idx_returns_user ON ordering.return_requests (user_id);


-- ============================================================================
-- TABLE: ordering.return_request_items
-- ============================================================================
CREATE TABLE ordering.return_request_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_request_id   UUID NOT NULL,
    order_line_item_id  UUID NOT NULL,
    return_quantity     INT NOT NULL DEFAULT 1,
    
    CONSTRAINT fk_return_items_request FOREIGN KEY (return_request_id) REFERENCES ordering.return_requests(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_return_items_line_item FOREIGN KEY (order_line_item_id) REFERENCES ordering.order_line_items(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_return_items_qty CHECK (return_quantity >= 1)
);

CREATE UNIQUE INDEX uq_return_items_unique ON ordering.return_request_items (return_request_id, order_line_item_id);
