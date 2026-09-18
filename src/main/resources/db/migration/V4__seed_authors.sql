-- ============================================================================
-- V4__seed_authors.sql
-- Online Bookstore Platform ("Book Corner")
-- Master Authors Seed Data, Book-Author Associations, and Foreign Key Enforcement
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Master Authors
-- ----------------------------------------------------------------------------
INSERT INTO catalog.authors (id, full_name, author_slug, biography, avatar_url) VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        'Robert C. Martin',
        'robert-c-martin',
        'Robert C. Martin (Uncle Bob) has been a programmer since 1970. He is cofounder of cleancoders.com, which offers video training for software developers, and a leading international consultant on agile software craftsmanship.',
        'https://images.bookcorner.com/authors/robert-c-martin.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        'Erich Gamma',
        'erich-gamma',
        'Erich Gamma is a Swiss computer scientist and co-author of the influential textbook Design Patterns: Elements of Reusable Object-Oriented Software. He is also recognized for his work on JUnit and Eclipse.',
        'https://images.bookcorner.com/authors/erich-gamma.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000003',
        'Richard Helm',
        'richard-helm',
        'Richard Helm is a member of the Gang of Four and a distinguished technology consultant specializing in object-oriented architecture and enterprise system engineering.',
        'https://images.bookcorner.com/authors/richard-helm.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000004',
        'Ralph Johnson',
        'ralph-johnson',
        'Ralph Johnson is a research associate professor in the Department of Computer Science at the University of Illinois at Urbana-Champaign and a co-author of Design Patterns.',
        'https://images.bookcorner.com/authors/ralph-johnson.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000005',
        'John Vlissides',
        'john-vlissides',
        'John Matthew Vlissides was an American software engineer and researcher at the IBM Thomas J. Watson Research Center, best known as one of the Gang of Four.',
        'https://images.bookcorner.com/authors/john-vlissides.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000006',
        'Martin Kleppmann',
        'martin-kleppmann',
        'Martin Kleppmann is an Associate Professor in Computer Science at the University of Cambridge, specializing in distributed systems, local-first software, and database reliability.',
        'https://images.bookcorner.com/authors/martin-kleppmann.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000007',
        'David Thomas',
        'david-thomas',
        'David Thomas is a computer programmer, author, and publisher. He co-authored The Pragmatic Programmer and was one of the original authors of the Agile Manifesto.',
        'https://images.bookcorner.com/authors/david-thomas.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000008',
        'Andrew Hunt',
        'andrew-hunt',
        'Andrew Hunt is a programmer and author of books on software development. He co-authored The Pragmatic Programmer and co-founded The Pragmatic Bookshelf.',
        'https://images.bookcorner.com/authors/andrew-hunt.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000009',
        'Frank Herbert',
        'frank-herbert',
        'Frank Patrick Herbert Jr. was an American science fiction author best known for the 1965 novel Dune and its five sequels. The Dune saga is widely considered the greatest science fiction masterpiece of all time.',
        'https://images.bookcorner.com/authors/frank-herbert.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000010',
        'J.R.R. Tolkien',
        'jrr-tolkien',
        'John Ronald Reuel Tolkien was an English writer, poet, philologist, and academic, best known as the author of the high fantasy works The Hobbit and The Lord of the Rings.',
        'https://images.bookcorner.com/authors/jrr-tolkien.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000011',
        'Andy Weir',
        'andy-weir',
        'Andy Weir is an American novelist and former software engineer. His 2011 novel The Martian and 2021 novel Project Hail Mary were both international bestsellers praised for their scientific rigor.',
        'https://images.bookcorner.com/authors/andy-weir.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000012',
        'Yuval Noah Harari',
        'yuval-noah-harari',
        'Yuval Noah Harari is an Israeli historian, philosopher, and bestselling author of Sapiens: A Brief History of Humankind, Homo Deus: A Brief History of Tomorrow, and 21 Lessons for the 21st Century.',
        'https://images.bookcorner.com/authors/yuval-noah-harari.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000013',
        'Walter Isaacson',
        'walter-isaacson',
        'Walter Isaacson is an American author, journalist, and professor. He has written biographies of Steve Jobs, Albert Einstein, Benjamin Franklin, and Leonardo da Vinci.',
        'https://images.bookcorner.com/authors/walter-isaacson.jpg'
    ),
    (
        '40000000-0000-0000-0000-000000000014',
        'James Clear',
        'james-clear',
        'James Clear is an author and speaker focused on habits, decision making, and continuous improvement. He is the author of the #1 New York Times bestseller Atomic Habits.',
        'https://images.bookcorner.com/authors/james-clear.jpg'
    )
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Book-Author Associations
-- ----------------------------------------------------------------------------
INSERT INTO catalog.book_authors (book_id, author_id, contribution_role, author_sequence) VALUES
    -- Book 1: Clean Code
    ('30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'AUTHOR', 1),

    -- Book 2: Design Patterns (Gang of Four)
    ('30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'AUTHOR', 1),
    ('30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000003', 'CO_AUTHOR', 2),
    ('30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000004', 'CO_AUTHOR', 3),
    ('30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000005', 'CO_AUTHOR', 4),

    -- Book 3: Designing Data-Intensive Applications
    ('30000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000006', 'AUTHOR', 1),

    -- Book 4: The Pragmatic Programmer
    ('30000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000007', 'AUTHOR', 1),
    ('30000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000008', 'CO_AUTHOR', 2),

    -- Book 5: Dune
    ('30000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000009', 'AUTHOR', 1),

    -- Book 6: The Hobbit
    ('30000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000010', 'AUTHOR', 1),

    -- Book 7: Project Hail Mary
    ('30000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000011', 'AUTHOR', 1),

    -- Book 8: Sapiens
    ('30000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000012', 'AUTHOR', 1),

    -- Book 9: Steve Jobs
    ('30000000-0000-0000-0000-000000000009', '40000000-0000-0000-0000-000000000013', 'AUTHOR', 1),

    -- Book 10: Atomic Habits
    ('30000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000014', 'AUTHOR', 1)
ON CONFLICT (book_id, author_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Enforce Foreign Key: catalog.book_authors -> catalog.authors
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_book_authors_author'
    ) THEN
        ALTER TABLE catalog.book_authors 
            ADD CONSTRAINT fk_book_authors_author 
            FOREIGN KEY (author_id) REFERENCES catalog.authors(id) 
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;
END $$;
