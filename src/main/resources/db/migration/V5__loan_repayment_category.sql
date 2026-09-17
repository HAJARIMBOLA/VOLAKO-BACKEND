INSERT INTO categories (user_id, name, type, is_system, is_active)
SELECT id, 'Remboursement crédit', 'EXPENSE', TRUE, TRUE
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.user_id = u.id AND c.name = 'Remboursement crédit' AND c.type = 'EXPENSE'
);
