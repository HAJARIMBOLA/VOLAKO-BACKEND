-- Registration now collects first name, last name, phone number AND email.
-- Still pre-production V1 data (local/dev test accounts only), so we reshape
-- the users table cleanly rather than backfilling placeholder values.
DELETE FROM refresh_tokens;
DELETE FROM transactions;
DELETE FROM categories;
DELETE FROM accounts;
DELETE FROM users;

ALTER TABLE users RENAME COLUMN full_name TO last_name;
ALTER TABLE users ADD COLUMN first_name VARCHAR(255) NOT NULL;
ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
