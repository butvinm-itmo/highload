-- Create role table
CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert default roles with fixed UUIDs
INSERT INTO role (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'USER'),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN');

-- Add authentication fields to user table
ALTER TABLE "user" ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE "user" ADD COLUMN role_id UUID REFERENCES role(id);
