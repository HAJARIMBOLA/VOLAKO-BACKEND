-- Phone number replaces email as the user's login identifier (Madagascar-first UX:
-- phone numbers are more universally used than email addresses here).
ALTER TABLE users RENAME COLUMN email TO phone_number;
