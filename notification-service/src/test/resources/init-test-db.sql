-- Initialize test database for notification-service tests
-- Only creates dependencies - Flyway handles notification table

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- User table (dependency for notification FK)
-- Required because notification-service doesn't manage the user table
CREATE TABLE IF NOT EXISTS "user" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(128) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Insert test users
INSERT INTO "user" (id, username) VALUES
    ('00000000-0000-0000-0000-000000000001', 'test_user_1'),
    ('00000000-0000-0000-0000-000000000002', 'test_user_2');
