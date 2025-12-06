-- Initialize test database with all tables for divination-service tests

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- User table (from user-service)
CREATE TABLE IF NOT EXISTS "user" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(128) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Arcana type table (from tarot-service)
CREATE TABLE IF NOT EXISTS arcana_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(16) NOT NULL
);

-- Layout type table (from tarot-service)
CREATE TABLE IF NOT EXISTS layout_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(32) NOT NULL,
    cards_count INTEGER NOT NULL,
    CONSTRAINT check_cards_count CHECK (cards_count > 0)
);

-- Card table (from tarot-service)
CREATE TABLE IF NOT EXISTS card (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    arcana_type_id UUID NOT NULL,
    CONSTRAINT fk_card_arcana_type FOREIGN KEY (arcana_type_id)
        REFERENCES arcana_type(id) ON DELETE RESTRICT
);

-- Spread table (divination-service)
CREATE TABLE IF NOT EXISTS spread (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question TEXT,
    layout_type_id UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    author_id UUID NOT NULL,
    CONSTRAINT fk_spread_layout_type FOREIGN KEY (layout_type_id)
        REFERENCES layout_type(id) ON DELETE RESTRICT,
    CONSTRAINT fk_spread_author FOREIGN KEY (author_id)
        REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_spread_author_id ON spread(author_id);
CREATE INDEX IF NOT EXISTS idx_spread_created_at ON spread(created_at DESC);

-- Spread card table (divination-service)
CREATE TABLE IF NOT EXISTS spread_card (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    spread_id UUID NOT NULL,
    card_id UUID NOT NULL,
    position_in_spread INTEGER NOT NULL,
    is_reversed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_spread_card_spread FOREIGN KEY (spread_id)
        REFERENCES spread(id) ON DELETE CASCADE,
    CONSTRAINT fk_spread_card_card FOREIGN KEY (card_id)
        REFERENCES card(id) ON DELETE RESTRICT,
    CONSTRAINT check_position CHECK (position_in_spread > 0)
);

CREATE INDEX IF NOT EXISTS idx_spread_card_spread_id ON spread_card(spread_id);
CREATE INDEX IF NOT EXISTS idx_spread_card_card_id ON spread_card(card_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_spread_card_unique_position ON spread_card(spread_id, position_in_spread);

-- Interpretation table (divination-service)
CREATE TABLE IF NOT EXISTS interpretation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    author_id UUID NOT NULL,
    spread_id UUID NOT NULL,
    CONSTRAINT fk_interpretation_author FOREIGN KEY (author_id)
        REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_interpretation_spread FOREIGN KEY (spread_id)
        REFERENCES spread(id) ON DELETE CASCADE,
    CONSTRAINT uq_interpretation_author_spread UNIQUE (author_id, spread_id)
);

CREATE INDEX IF NOT EXISTS idx_interpretation_spread_id ON interpretation(spread_id);
CREATE INDEX IF NOT EXISTS idx_interpretation_author_id ON interpretation(author_id);

-- Insert test data

-- Test user
INSERT INTO "user" (id, username) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin');

-- Arcana types
INSERT INTO arcana_type (id, name) VALUES
    ('00000000-0000-0000-0000-000000000010', 'MAJOR'),
    ('00000000-0000-0000-0000-000000000011', 'MINOR');

-- Layout types
INSERT INTO layout_type (id, name, cards_count) VALUES
    ('00000000-0000-0000-0000-000000000020', 'ONE_CARD', 1),
    ('00000000-0000-0000-0000-000000000021', 'THREE_CARDS', 3),
    ('00000000-0000-0000-0000-000000000022', 'CROSS', 5);

-- Cards
INSERT INTO card (id, name, arcana_type_id) VALUES
    ('00000000-0000-0000-0000-000000000030', 'The Fool', '00000000-0000-0000-0000-000000000010'),
    ('00000000-0000-0000-0000-000000000031', 'The Magician', '00000000-0000-0000-0000-000000000010'),
    ('00000000-0000-0000-0000-000000000032', 'The High Priestess', '00000000-0000-0000-0000-000000000010'),
    ('00000000-0000-0000-0000-000000000033', 'The Empress', '00000000-0000-0000-0000-000000000010'),
    ('00000000-0000-0000-0000-000000000034', 'The Emperor', '00000000-0000-0000-0000-000000000010');
