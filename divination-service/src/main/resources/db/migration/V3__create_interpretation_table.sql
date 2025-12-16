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
