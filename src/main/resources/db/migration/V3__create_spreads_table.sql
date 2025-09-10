-- Create spreads table
CREATE TABLE spreads (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question VARCHAR(1000) NOT NULL,
    layout_type VARCHAR(15) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    author_id BIGINT NOT NULL,
    CONSTRAINT fk_spreads_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_layout_type CHECK (layout_type IN ('ONE_CARD', 'THREE_CARDS', 'CROSS'))
);

-- Create indexes
CREATE INDEX idx_spreads_created_at ON spreads (created_at DESC);
CREATE INDEX idx_spreads_author_id ON spreads (author_id);