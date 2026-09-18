-- ============================================================================
-- V5__seed_publishers.sql
-- Online Bookstore Platform ("Book Corner")
-- Master Publishers Seed Data and Book-Publisher Foreign Key Enforcement
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Master Publishers
-- ----------------------------------------------------------------------------
INSERT INTO catalog.publishers (
    id, publisher_name, publisher_code, contact_email, website_url
) VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'Pearson Education / Prentice Hall',
        'PEARSON_PRENTICE_HALL',
        'orders@pearson.com',
        'https://www.pearson.com'
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'Addison-Wesley Professional',
        'ADDISON_WESLEY',
        'support@informit.com',
        'https://www.informit.com/aw'
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        'O''Reilly Media',
        'OREILLY_MEDIA',
        'customer-service@oreilly.com',
        'https://www.oreilly.com'
    ),
    (
        '20000000-0000-0000-0000-000000000004',
        'Ace Books / Penguin Random House',
        'ACE_PENGUIN',
        'consumerservices@penguinrandomhouse.com',
        'https://www.penguinrandomhouse.com'
    ),
    (
        '20000000-0000-0000-0000-000000000005',
        'Houghton Mifflin Harcourt',
        'HOUGHTON_MIFFLIN_HARCOURT',
        'tradeorders@hmhco.com',
        'https://www.hmhco.com'
    ),
    (
        '20000000-0000-0000-0000-000000000006',
        'Ballantine Books',
        'BALLANTINE_BOOKS',
        'ballantine@penguinrandomhouse.com',
        'https://www.penguinrandomhouse.com'
    ),
    (
        '20000000-0000-0000-0000-000000000007',
        'HarperCollins Publishers',
        'HARPERCOLLINS',
        'orders@harpercollins.com',
        'https://www.harpercollins.com'
    ),
    (
        '20000000-0000-0000-0000-000000000008',
        'Simon & Schuster',
        'SIMON_SCHUSTER',
        'customer.service@simonandschuster.com',
        'https://www.simonandschuster.com'
    ),
    (
        '20000000-0000-0000-0000-000000000009',
        'Avery / Penguin Publishing Group',
        'AVERY_PUBLISHING',
        'avery@penguinrandomhouse.com',
        'https://www.penguinrandomhouse.com'
    )
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Enforce Foreign Key: catalog.books -> catalog.publishers
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_books_publisher'
    ) THEN
        ALTER TABLE catalog.books 
            ADD CONSTRAINT fk_books_publisher 
            FOREIGN KEY (publisher_id) REFERENCES catalog.publishers(id) 
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_books_publisher 
    ON catalog.books (publisher_id) 
    WHERE is_deleted = FALSE;
