CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
