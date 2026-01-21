CREATE TABLE IF NOT EXISTS interpretation_attachment (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interpretation_id UUID NOT NULL UNIQUE,
    file_upload_id UUID NOT NULL,
    original_file_name VARCHAR(256) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_interpretation FOREIGN KEY (interpretation_id)
        REFERENCES interpretation(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_attachment_interpretation_id ON interpretation_attachment(interpretation_id);
CREATE INDEX IF NOT EXISTS idx_attachment_file_upload_id ON interpretation_attachment(file_upload_id);
