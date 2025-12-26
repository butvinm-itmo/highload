-- Initialize test database with divination-service tables only
-- Note: user, layout_type, and card tables removed - tests mock Feign client responses with WireMock

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Spread table (divination-service)
-- Note: Cross-service FK constraints removed to enable independent database deployment
CREATE TABLE IF NOT EXISTS spread (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question TEXT,
    layout_type_id UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    author_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spread_author_id ON spread(author_id);
CREATE INDEX IF NOT EXISTS idx_spread_created_at ON spread(created_at DESC);

-- Spread card table (divination-service)
-- Note: Cross-service FK constraint (fk_spread_card_card) removed
CREATE TABLE IF NOT EXISTS spread_card (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    spread_id UUID NOT NULL,
    card_id UUID NOT NULL,
    position_in_spread INTEGER NOT NULL,
    is_reversed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_spread_card_spread FOREIGN KEY (spread_id)
        REFERENCES spread(id) ON DELETE CASCADE,
    CONSTRAINT check_position CHECK (position_in_spread > 0)
);

CREATE INDEX IF NOT EXISTS idx_spread_card_spread_id ON spread_card(spread_id);
CREATE INDEX IF NOT EXISTS idx_spread_card_card_id ON spread_card(card_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_spread_card_unique_position ON spread_card(spread_id, position_in_spread);

-- Interpretation table (divination-service)
-- Note: Cross-service FK constraint (fk_interpretation_author) removed
CREATE TABLE IF NOT EXISTS interpretation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    author_id UUID NOT NULL,
    spread_id UUID NOT NULL,
    CONSTRAINT fk_interpretation_spread FOREIGN KEY (spread_id)
        REFERENCES spread(id) ON DELETE CASCADE,
    CONSTRAINT uq_interpretation_author_spread UNIQUE (author_id, spread_id)
);

CREATE INDEX IF NOT EXISTS idx_interpretation_spread_id ON interpretation(spread_id);
CREATE INDEX IF NOT EXISTS idx_interpretation_author_id ON interpretation(author_id);
