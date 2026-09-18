-- ============================================================================
-- V2__seed_categories.sql
-- Online Bookstore Platform ("Book Corner")
-- Hierarchical Product Categories Seed Data (Self-referential tree structure)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Root Level Categories (Tree Level 0)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_category_id, category_name, category_slug, tree_level, display_order) VALUES
    ('10000000-0000-0000-0000-000000000001', NULL, 'Fiction', 'fiction', 0, 1),
    ('10000000-0000-0000-0000-000000000002', NULL, 'Non-Fiction', 'non-fiction', 0, 2),
    ('10000000-0000-0000-0000-000000000003', NULL, 'Science & Technology', 'science-tech', 0, 3),
    ('10000000-0000-0000-0000-000000000004', NULL, 'Children & Young Adult', 'young-adult', 0, 4)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Subcategories: Fiction (Tree Level 1)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_category_id, category_name, category_slug, tree_level, display_order) VALUES
    ('10000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'Science Fiction', 'sci-fi', 1, 1),
    ('10000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'Fantasy', 'fantasy', 1, 2),
    ('10000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'Mystery & Thriller', 'mystery-thriller', 1, 3),
    ('10000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'Literary Fiction', 'literary-fiction', 1, 4)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Subcategories: Non-Fiction (Tree Level 1)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_category_id, category_name, category_slug, tree_level, display_order) VALUES
    ('10000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'Biography & Memoir', 'biography', 1, 1),
    ('10000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'History & Civilization', 'history', 1, 2),
    ('10000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000002', 'Personal Development', 'self-help', 1, 3),
    ('10000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000002', 'Business & Leadership', 'business-leadership', 1, 4)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. Subcategories: Science & Technology (Tree Level 1)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_category_id, category_name, category_slug, tree_level, display_order) VALUES
    ('10000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000003', 'Computer Science & Software Engineering', 'computer-science', 1, 1),
    ('10000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000003', 'System Architecture & Cloud', 'system-architecture', 1, 2)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. Subcategories: Children & Young Adult (Tree Level 1)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_category_id, category_name, category_slug, tree_level, display_order) VALUES
    ('10000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000004', 'YA Fantasy', 'ya-fantasy', 1, 1)
ON CONFLICT (id) DO NOTHING;
