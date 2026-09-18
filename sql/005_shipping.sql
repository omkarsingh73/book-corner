-- ============================================================================
-- 005_shipping.sql
-- Online Bookstore Platform ("Book Corner")
-- Schema: shipping
-- Description: Carrier rate cards, dynamic freight calculation matrix,
--              shipping consignments, and milestone checkpoint tracking.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS shipping;

-- ============================================================================
-- TABLE: shipping.carrier_rate_cards
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
    
    -- Audit & Concurrency Columns
    version                     BIGINT NOT NULL DEFAULT 0,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT chk_rate_cards_weight CHECK (max_weight_grams > 0),
    CONSTRAINT chk_rate_cards_base_rate CHECK (base_rate_amount >= 0),
    CONSTRAINT chk_rate_cards_per_kg CHECK (per_kg_rate_amount >= 0),
    CONSTRAINT chk_rate_cards_transit_min CHECK (estimated_transit_days_min >= 1),
    CONSTRAINT chk_rate_cards_transit_max CHECK (estimated_transit_days_max >= estimated_transit_days_min)
);

CREATE UNIQUE INDEX uq_carrier_rates_lookup 
    ON shipping.carrier_rate_cards (
        carrier_code, service_level, origin_zone, 
        destination_postal_prefix, max_weight_grams
    ) 
    WHERE is_deleted = FALSE;

CREATE INDEX idx_carrier_rate_match 
    ON shipping.carrier_rate_cards (
        carrier_code, service_level, destination_postal_prefix, max_weight_grams
    ) 
    WHERE is_active = TRUE AND is_deleted = FALSE;


-- ============================================================================
-- TABLE: shipping.shipping_consignments
-- ============================================================================
CREATE TABLE shipping.shipping_consignments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                    UUID NOT NULL,
    carrier_code                VARCHAR(64) NOT NULL,
    tracking_number             VARCHAR(128) NOT NULL,
    service_level               VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
    consignment_status          VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    total_weight_grams          INT NOT NULL,
    shipping_label_url          VARCHAR(512) NULL,
    estimated_delivery_min_at   TIMESTAMPTZ NOT NULL,
    estimated_delivery_max_at   TIMESTAMPTZ NOT NULL,
    dispatched_at               TIMESTAMPTZ NULL,
    delivered_at                TIMESTAMPTZ NULL,
    
    -- Audit & Concurrency Columns
    version                     BIGINT NOT NULL DEFAULT 0,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_shipping_consignments_order FOREIGN KEY (order_id) REFERENCES ordering.orders(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_consignments_status CHECK (consignment_status IN (
        'CREATED', 'MANIFESTED', 'DISPATCHED', 'IN_TRANSIT', 
        'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNED_TO_ORIGIN'
    )),
    CONSTRAINT chk_consignments_weight CHECK (total_weight_grams > 0),
    CONSTRAINT chk_consignments_eta CHECK (estimated_delivery_max_at >= estimated_delivery_min_at)
);

CREATE UNIQUE INDEX uq_consignments_tracking ON shipping.shipping_consignments (tracking_number);
CREATE INDEX idx_consignments_order ON shipping.shipping_consignments (order_id);
CREATE INDEX idx_consignments_status ON shipping.shipping_consignments (consignment_status);


-- ============================================================================
-- TABLE: shipping.tracking_milestones
-- ============================================================================
CREATE TABLE shipping.tracking_milestones (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consignment_id    UUID NOT NULL,
    milestone_status  VARCHAR(64) NOT NULL,
    location_city     VARCHAR(100) NULL,
    location_country  VARCHAR(2) NULL,
    carrier_timestamp TIMESTAMPTZ NOT NULL,
    carrier_remarks   VARCHAR(512) NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_tracking_milestones_consignment FOREIGN KEY (consignment_id) REFERENCES shipping.shipping_consignments(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_milestones_country CHECK (location_country IS NULL OR length(location_country) = 2)
);

CREATE INDEX idx_tracking_milestones_consignment 
    ON shipping.tracking_milestones (consignment_id, carrier_timestamp DESC);
