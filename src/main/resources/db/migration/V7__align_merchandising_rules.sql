-- ============================================================================
-- V7__align_merchandising_rules.sql
-- Online Bookstore Platform ("Book Corner")
-- Adds is_active flag and aligns merchandising_rules schema with entity model
-- ============================================================================

ALTER TABLE catalog.merchandising_rules 
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE catalog.merchandising_rules 
    ADD COLUMN IF NOT EXISTS discount_bundle_pct INT NULL;
