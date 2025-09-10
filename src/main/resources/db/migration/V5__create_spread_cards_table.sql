-- Create spread_cards table
CREATE TABLE spread_cards (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    spread_id BIGINT NOT NULL,
    card_id INT NOT NULL,
    position_in_spread INT NOT NULL,
    is_reversed BOOLEAN NOT NULL,
    CONSTRAINT fk_spread_cards_spread FOREIGN KEY (spread_id) REFERENCES spreads(id) ON DELETE CASCADE,
    CONSTRAINT fk_spread_cards_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

-- Create indexes
CREATE INDEX idx_spread_cards_spread_id ON spread_cards (spread_id);
CREATE INDEX idx_spread_cards_card_id ON spread_cards (card_id);