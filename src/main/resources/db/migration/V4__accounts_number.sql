-- Lets a user tell apart multiple accounts of the same type (e.g. two MVola
-- numbers, or several bank accounts): a bank account number or a mobile
-- money phone number. Optional — cash accounts have no use for it.
ALTER TABLE accounts ADD COLUMN account_number VARCHAR(50);
