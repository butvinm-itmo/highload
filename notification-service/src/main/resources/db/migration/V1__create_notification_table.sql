CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL,
    interpretation_id UUID NOT NULL UNIQUE,
    interpretation_author_id UUID NOT NULL,
    spread_id UUID NOT NULL,
    title VARCHAR(256) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_read_created
    ON notification(recipient_id, is_read, created_at DESC);
