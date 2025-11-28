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

CREATE UNIQUE INDEX IF NOT EXISTS idx_spread_card_unique_position
    ON spread_card(spread_id, position_in_spread);
