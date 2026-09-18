-- ============================================================================
-- V3__seed_books.sql
-- Online Bookstore Platform ("Book Corner")
-- Master Product Catalog: Books, Format SKUs, and Merchandising Rules
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Master Books
-- ----------------------------------------------------------------------------
INSERT INTO catalog.books (
    id, isbn_13, isbn_10, title, subtitle,
    publisher_id, primary_category_id, language,
    publication_date, edition, page_count, synopsis, cover_image_url
) VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '9780132350884', '0132350882',
        'Clean Code', 'A Handbook of Agile Software Craftsmanship',
        '20000000-0000-0000-0000-000000000001', -- Pearson
        '10000000-0000-0000-0000-000000000031', -- Computer Science
        'en', '2008-08-01', '1st Edition', 464,
        'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost because of poorly written code. But it doesn''t have to be that way.',
        'https://images.bookcorner.com/covers/clean-code.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '9780201633610', '0201633612',
        'Design Patterns', 'Elements of Reusable Object-Oriented Software',
        '20000000-0000-0000-0000-000000000002', -- Addison-Wesley
        '10000000-0000-0000-0000-000000000031', -- Computer Science
        'en', '1994-11-10', '1st Edition', 416,
        'Capturing a wealth of experience about the design of object-oriented software, four top-notch designers present a catalog of simple and succinct solutions to commonly occurring design problems.',
        'https://images.bookcorner.com/covers/design-patterns.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '9781449373320', '1449373321',
        'Designing Data-Intensive Applications', 'The Big Ideas Behind Reliable, Scalable, and Maintainable Systems',
        '20000000-0000-0000-0000-000000000003', -- O''Reilly Media
        '10000000-0000-0000-0000-000000000032', -- System Architecture
        'en', '2017-03-16', '1st Edition', 616,
        'Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, efficiency, and maintainability.',
        'https://images.bookcorner.com/covers/ddia.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000004',
        '9780135957059', '0135957052',
        'The Pragmatic Programmer', 'Your Journey To Mastery, 20th Anniversary Edition',
        '20000000-0000-0000-0000-000000000002', -- Addison-Wesley
        '10000000-0000-0000-0000-000000000031', -- Computer Science
        'en', '2019-09-13', '20th Anniversary Edition', 352,
        'The Pragmatic Programmer is one of those rare tech audiobooks you''ll listen, read, and refer to again and again over the years. Whether you''re new to the field or an experienced practitioner, you''ll come away each time with fresh insights.',
        'https://images.bookcorner.com/covers/pragmatic-programmer.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000005',
        '9780441172719', '0441172717',
        'Dune', 'The Epic Science Fiction Masterpiece',
        '20000000-0000-0000-0000-000000000004', -- Ace / Penguin
        '10000000-0000-0000-0000-000000000011', -- Science Fiction
        'en', '1965-08-01', 'Deluxe Edition', 896,
        'Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only thing of value is the "spice" melange.',
        'https://images.bookcorner.com/covers/dune.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000006',
        '9780547928227', '054792822X',
        'The Hobbit', 'There and Back Again',
        '20000000-0000-0000-0000-000000000005', -- Houghton Mifflin Harcourt
        '10000000-0000-0000-0000-000000000012', -- Fantasy
        'en', '1937-09-21', 'Collector''s Edition', 320,
        'Bilbo Baggins is a hobbit who enjoys a comfortable, unambitious life, rarely traveling any farther than his pantry or cellar. But his contentment is disturbed when the wizard Gandalf and a company of dwarves arrive on his doorstep.',
        'https://images.bookcorner.com/covers/the-hobbit.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000007',
        '9780593135204', '0593135202',
        'Project Hail Mary', 'A Novel',
        '20000000-0000-0000-0000-000000000006', -- Ballantine Books
        '10000000-0000-0000-0000-000000000011', -- Science Fiction
        'en', '2021-05-04', '1st Edition', 496,
        'Ryland Grace is the sole survivor on a desperate, last-chance mission—and if he fails, humanity and the earth itself are doomed. Except right now, he doesn''t know that. He can''t even remember his own name.',
        'https://images.bookcorner.com/covers/project-hail-mary.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000008',
        '9780062316097', '0062316095',
        'Sapiens', 'A Brief History of Humankind',
        '20000000-0000-0000-0000-000000000007', -- Harper
        '10000000-0000-0000-0000-000000000022', -- History
        'en', '2015-02-10', '1st Edition', 464,
        'One hundred thousand years ago, at least six different species of humans inhabited Earth. Yet today there is only one—homo sapiens. What happened to the others? And what may happen to us?',
        'https://images.bookcorner.com/covers/sapiens.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000009',
        '9781451648539', '1451648537',
        'Steve Jobs', 'The Exclusive Biography',
        '20000000-0000-0000-0000-000000000008', -- Simon & Schuster
        '10000000-0000-0000-0000-000000000021', -- Biography
        'en', '2011-10-24', '1st Edition', 656,
        'Based on more than forty interviews with Steve Jobs conducted over two years—as well as interviews with more than a hundred family members, friends, adversaries, competitors, and colleagues—Walter Isaacson has written a riveting story.',
        'https://images.bookcorner.com/covers/steve-jobs.jpg'
    ),
    (
        '30000000-0000-0000-0000-000000000010',
        '9780735211292', '0735211299',
        'Atomic Habits', 'An Easy & Proven Way to Build Good Habits & Break Bad Ones',
        '20000000-0000-0000-0000-000000000009', -- Avery
        '10000000-0000-0000-0000-000000000023', -- Personal Development
        'en', '2018-10-16', '1st Edition', 320,
        'No matter your goals, Atomic Habits offers a proven framework for improving—every day. James Clear, one of the world''s leading experts on habit formation, reveals practical strategies that will teach you exactly how to form good habits, break bad ones, and master the tiny behaviors that lead to remarkable results.',
        'https://images.bookcorner.com/covers/atomic-habits.jpg'
    )
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Format SKUs for each Book
-- ----------------------------------------------------------------------------
INSERT INTO catalog.book_formats (
    id, book_id, sku, format_type,
    base_price_amount, currency_code, weight_grams,
    length_mm, width_mm, thickness_mm, stock_quantity
) VALUES
    -- Book 1: Clean Code
    ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'SKU-CC-PB', 'PAPERBACK', 3999, 'USD', 680, 234, 178, 25, 250),
    ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'SKU-CC-EB', 'EBOOK', 2499, 'USD', 0, NULL, NULL, NULL, 999999),
    ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 'SKU-CC-HC', 'HARDCOVER', 4999, 'USD', 850, 240, 185, 28, 85),

    -- Book 2: Design Patterns
    ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', 'SKU-DP-HC', 'HARDCOVER', 5499, 'USD', 920, 240, 190, 30, 120),
    ('31000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002', 'SKU-DP-EB', 'EBOOK', 2999, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 3: Designing Data-Intensive Applications
    ('31000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000003', 'SKU-DDIA-PB', 'PAPERBACK', 4499, 'USD', 890, 233, 178, 33, 340),
    ('31000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000003', 'SKU-DDIA-EB', 'EBOOK', 2799, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 4: The Pragmatic Programmer
    ('31000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000004', 'SKU-PP-HC', 'HARDCOVER', 4799, 'USD', 710, 235, 185, 24, 180),
    ('31000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000004', 'SKU-PP-EB', 'EBOOK', 2699, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 5: Dune
    ('31000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000005', 'SKU-DUNE-PB', 'PAPERBACK', 1899, 'USD', 480, 190, 106, 32, 500),
    ('31000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000005', 'SKU-DUNE-HC', 'HARDCOVER', 2999, 'USD', 750, 230, 150, 40, 220),
    ('31000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000005', 'SKU-DUNE-AB', 'AUDIOBOOK', 2199, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 6: The Hobbit
    ('31000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000006', 'SKU-HOB-PB', 'PAPERBACK', 1499, 'USD', 340, 208, 140, 20, 420),
    ('31000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000006', 'SKU-HOB-HC', 'HARDCOVER', 2799, 'USD', 580, 215, 145, 25, 160),

    -- Book 7: Project Hail Mary
    ('31000000-0000-0000-0000-000000000015', '30000000-0000-0000-0000-000000000007', 'SKU-PHM-HC', 'HARDCOVER', 2899, 'USD', 650, 235, 155, 35, 310),
    ('31000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000007', 'SKU-PHM-EB', 'EBOOK', 1499, 'USD', 0, NULL, NULL, NULL, 999999),
    ('31000000-0000-0000-0000-000000000017', '30000000-0000-0000-0000-000000000007', 'SKU-PHM-AB', 'AUDIOBOOK', 2499, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 8: Sapiens
    ('31000000-0000-0000-0000-000000000018', '30000000-0000-0000-0000-000000000008', 'SKU-SAP-PB', 'PAPERBACK', 2199, 'USD', 520, 229, 152, 28, 280),
    ('31000000-0000-0000-0000-000000000019', '30000000-0000-0000-0000-000000000008', 'SKU-SAP-EB', 'EBOOK', 1399, 'USD', 0, NULL, NULL, NULL, 999999),

    -- Book 9: Steve Jobs
    ('31000000-0000-0000-0000-000000000020', '30000000-0000-0000-0000-000000000009', 'SKU-JOBS-HC', 'HARDCOVER', 3500, 'USD', 950, 235, 155, 45, 190),
    ('31000000-0000-0000-0000-000000000021', '30000000-0000-0000-0000-000000000009', 'SKU-JOBS-PB', 'PAPERBACK', 1999, 'USD', 700, 210, 140, 38, 300),

    -- Book 10: Atomic Habits
    ('31000000-0000-0000-0000-000000000022', '30000000-0000-0000-0000-000000000010', 'SKU-ATOM-HC', 'HARDCOVER', 2700, 'USD', 560, 230, 155, 27, 450),
    ('31000000-0000-0000-0000-000000000023', '30000000-0000-0000-0000-000000000010', 'SKU-ATOM-EB', 'EBOOK', 1299, 'USD', 0, NULL, NULL, NULL, 999999)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Merchandising Rules (Cross-sell, Up-sell, Bundles)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.merchandising_rules (
    id, source_book_id, target_book_id, rule_type, priority_score, discount_percentage
) VALUES
    -- Clean Code -> The Pragmatic Programmer (Cross-Sell 15% discount)
    ('32000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000004', 'CROSS_SELL', 100, 15.00),

    -- Clean Code + Design Patterns (Bundle 20% discount)
    ('32000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'BUNDLE', 90, 20.00),

    -- DDIA -> The Pragmatic Programmer (Up-Sell 10% discount)
    ('32000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004', 'UP_SELL', 80, 10.00),

    -- Dune -> Project Hail Mary (Cross-Sell 10% discount)
    ('32000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000007', 'CROSS_SELL', 95, 10.00),

    -- Sapiens + Atomic Habits (Bundle 15% discount)
    ('32000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000010', 'BUNDLE', 85, 15.00)
ON CONFLICT (id) DO NOTHING;
