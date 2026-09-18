-- ============================================================================
-- run_all.sql
-- Master DDL Migration Runner for "Book Corner"
-- Executes all 7 DDL migration scripts in strict dependency order.
-- ============================================================================

\echo 'Executing 001_users.sql...'
\i 001_users.sql

\echo 'Executing 002_catalog.sql...'
\i 002_catalog.sql

\echo 'Executing 003_orders.sql...'
\i 003_orders.sql

\echo 'Executing 004_payments.sql...'
\i 004_payments.sql

\echo 'Executing 005_shipping.sql...'
\i 005_shipping.sql

\echo 'Executing 006_reviews.sql...'
\i 006_reviews.sql

\echo 'Executing 007_coupons.sql...'
\i 007_coupons.sql

\echo 'All DDL migrations applied successfully!'
