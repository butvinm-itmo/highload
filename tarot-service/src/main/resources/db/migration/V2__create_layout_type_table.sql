CREATE TABLE layout_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(32) NOT NULL,
    cards_count INTEGER NOT NULL,
    CONSTRAINT check_cards_count CHECK (cards_count > 0)
);
