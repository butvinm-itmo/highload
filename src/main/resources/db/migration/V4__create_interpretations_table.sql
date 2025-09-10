-- Create interpretations table
CREATE TABLE interpretations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    author_id BIGINT NOT NULL,
    spread_id BIGINT NOT NULL,
    CONSTRAINT fk_interpretations_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_interpretations_spread FOREIGN KEY (spread_id) REFERENCES spreads(id) ON DELETE CASCADE,
    CONSTRAINT uk_interpretations_author_spread UNIQUE (author_id, spread_id)
);

-- Create indexes
CREATE INDEX idx_interpretations_spread_id ON interpretations (spread_id);
CREATE INDEX idx_interpretations_author_id ON interpretations (author_id);