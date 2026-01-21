-- Ensure UUID extension exists (may already be created by other services)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS file_upload (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    original_file_name VARCHAR(256) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_file_upload_user_id ON file_upload(user_id);
CREATE INDEX IF NOT EXISTS idx_file_upload_status ON file_upload(status);
CREATE INDEX IF NOT EXISTS idx_file_upload_expires_at ON file_upload(expires_at) WHERE status = 'PENDING';
