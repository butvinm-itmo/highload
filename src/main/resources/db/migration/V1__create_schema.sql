CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(128) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE layout_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(32) NOT NULL,
    cards_count INTEGER NOT NULL,
    CONSTRAINT check_cards_count CHECK (cards_count > 0)
);

CREATE TABLE arcana_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(16) NOT NULL
);

CREATE TABLE card (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    arcana_type_id UUID NOT NULL,
    CONSTRAINT fk_card_arcana_type FOREIGN KEY (arcana_type_id)
        REFERENCES arcana_type(id) ON DELETE RESTRICT
);

CREATE TABLE spread (
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

CREATE TABLE spread_card (
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

CREATE TABLE interpretation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    author_id UUID NOT NULL,
    spread_id UUID NOT NULL,
    CONSTRAINT fk_interpretation_author FOREIGN KEY (author_id)
        REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_interpretation_spread FOREIGN KEY (spread_id)
        REFERENCES spread(id) ON DELETE CASCADE
);

CREATE INDEX idx_spread_author_id ON spread(author_id);
CREATE INDEX idx_spread_created_at ON spread(created_at DESC);
CREATE INDEX idx_spread_card_spread_id ON spread_card(spread_id);
CREATE INDEX idx_spread_card_card_id ON spread_card(card_id);
CREATE INDEX idx_interpretation_spread_id ON interpretation(spread_id);
CREATE INDEX idx_interpretation_author_id ON interpretation(author_id);
CREATE INDEX idx_card_arcana_type_id ON card(arcana_type_id);

CREATE UNIQUE INDEX idx_spread_card_unique_position
    ON spread_card(spread_id, position_in_spread);
