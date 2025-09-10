-- Create cards table
CREATE TABLE cards (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    arcana_type VARCHAR(10) NOT NULL,
    CONSTRAINT chk_arcana_type CHECK (arcana_type IN ('MAJOR', 'MINOR'))
);