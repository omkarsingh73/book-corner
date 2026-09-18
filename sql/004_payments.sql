-- ============================================================================
-- 004_payments.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: payment
-- Description: Payment transactions, multi-tender splits, double-entry customer
--              wallet ledger, gift vouchers, and refund tracking.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS payment;

-- ============================================================================
-- TABLE: payment.payment_transactions
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
    
    -- Audit & Concurrency Columns
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_payment_transactions_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_payment_tx_status CHECK (transaction_status IN (
        'INITIALIZED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'VOIDED', 'REFUNDED'
    )),
    CONSTRAINT chk_payment_tx_amount CHECK (total_payable_amount > 0),
    CONSTRAINT chk_payment_tx_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_payment_tx_idempotency ON payment.payment_transactions (idempotency_key);
CREATE INDEX idx_payment_tx_order ON payment.payment_transactions (order_id);
CREATE INDEX idx_payment_tx_status ON payment.payment_transactions (transaction_status);


-- ============================================================================
-- TABLE: payment.tender_splits (Multi-Tender)
-- ============================================================================
CREATE TABLE payment.tender_splits (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id  UUID NOT NULL,
    tender_type             VARCHAR(32) NOT NULL,
    amount                  BIGINT NOT NULL,
    tender_reference        VARCHAR(255) NULL,
    tender_status           VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED',
    
    CONSTRAINT fk_tender_splits_tx FOREIGN KEY (payment_transaction_id) REFERENCES payment.payment_transactions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_tender_splits_type CHECK (tender_type IN (
        'DIGITAL_WALLET', 'CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'GIFT_CARD'
    )),
    CONSTRAINT chk_tender_splits_amount CHECK (amount > 0),
    CONSTRAINT chk_tender_splits_status CHECK (tender_status IN (
        'PENDING', 'AUTHORIZED', 'CAPTURED', 'REVERSED'
    ))
);

CREATE INDEX idx_tender_splits_tx ON payment.tender_splits (payment_transaction_id);


-- ============================================================================
-- TABLE: payment.customer_wallets
-- ============================================================================
CREATE TABLE payment.customer_wallets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL,
    currency_code    VARCHAR(3) NOT NULL DEFAULT 'USD',
    current_balance  BIGINT NOT NULL DEFAULT 0,
    held_balance     BIGINT NOT NULL DEFAULT 0,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit & Concurrency Columns
    version          BIGINT NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_customer_wallets_user FOREIGN KEY (user_id) REFERENCES member.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_customer_wallets_balance CHECK (current_balance >= 0),
    CONSTRAINT chk_customer_wallets_held CHECK (held_balance >= 0),
    CONSTRAINT chk_customer_wallets_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_customer_wallets_user 
    ON payment.customer_wallets (user_id) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: payment.wallet_ledger_entries (Append-Only Double-Entry)
-- ============================================================================
CREATE TABLE payment.wallet_ledger_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id           UUID NOT NULL,
    entry_type          VARCHAR(32) NOT NULL,
    amount              BIGINT NOT NULL,
    balance_after       BIGINT NOT NULL,
    reference_order_id  UUID NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    remarks             VARCHAR(255) NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES payment.customer_wallets(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_wallet_ledger_type CHECK (entry_type IN (
        'CREDIT_TOPUP', 'DEBIT_PURCHASE', 'HOLD_AUTHORIZATION', 'HOLD_RELEASE', 'REFUND_CREDIT'
    )),
    CONSTRAINT chk_wallet_ledger_amount CHECK (amount <> 0),
    CONSTRAINT chk_wallet_ledger_balance_after CHECK (balance_after >= 0)
);

CREATE UNIQUE INDEX uq_wallet_ledger_idempotency ON payment.wallet_ledger_entries (idempotency_key);
CREATE INDEX idx_wallet_ledger_wallet_date ON payment.wallet_ledger_entries (wallet_id, created_at DESC);


-- ============================================================================
-- TABLE: payment.gift_cards
-- ============================================================================
CREATE TABLE payment.gift_cards (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_code_hash   VARCHAR(255) NOT NULL,
    initial_balance  BIGINT NOT NULL,
    current_balance  BIGINT NOT NULL,
    currency_code    VARCHAR(3) NOT NULL DEFAULT 'USD',
    expires_at       TIMESTAMPTZ NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit & Concurrency Columns
    version          BIGINT NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT chk_gift_cards_initial CHECK (initial_balance > 0),
    CONSTRAINT chk_gift_cards_balance CHECK (current_balance >= 0 AND current_balance <= initial_balance),
    CONSTRAINT chk_gift_cards_currency CHECK (length(currency_code) = 3)
);

CREATE UNIQUE INDEX uq_gift_cards_code_hash 
    ON payment.gift_cards (card_code_hash) 
    WHERE is_deleted = FALSE;


-- ============================================================================
-- TABLE: payment.refund_records
-- ============================================================================
CREATE TABLE payment.refund_records (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL,
    payment_transaction_id  UUID NOT NULL,
    return_request_id       UUID NULL,
    refund_amount           BIGINT NOT NULL,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'USD',
    refund_reason           VARCHAR(255) NOT NULL,
    destination_tender_type VARCHAR(32) NOT NULL,
    gateway_refund_id       VARCHAR(255) NULL,
    refund_status           VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    processed_at            TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_refund_records_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_refund_records_payment_tx FOREIGN KEY (payment_transaction_id) REFERENCES payment.payment_transactions(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_refund_records_rma FOREIGN KEY (return_request_id) REFERENCES ordering.return_requests(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_refund_amount CHECK (refund_amount > 0),
    CONSTRAINT chk_refund_destination CHECK (destination_tender_type IN ('ORIGINAL_GATEWAY', 'WALLET_CREDIT')),
    CONSTRAINT chk_refund_status CHECK (refund_status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_refund_records_order ON payment.refund_records (order_id);
CREATE INDEX idx_refund_records_tx ON payment.refund_records (payment_transaction_id);
