-- Delete all existing test users (fresh start)
-- Note: In production with shared database, divination-service tables will cascade delete via FK constraints
DELETE FROM "user";

-- Insert admin user with BCrypt hash for "Admin@123"
-- Hash: $2a$10$tCj/mvaQRu9jcd3TA0r2meeCpBXdWSeQqB25ni3LKIZ5g66kZ2226
-- role_id references ADMIN role (00000000-0000-0000-0000-000000000002)
INSERT INTO "user" (id, username, password_hash, role_id, created_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$tCj/mvaQRu9jcd3TA0r2meeCpBXdWSeQqB25ni3LKIZ5g66kZ2226',
    '00000000-0000-0000-0000-000000000002',
    CURRENT_TIMESTAMP
);

-- Make password_hash and role_id NOT NULL now that we have valid data
ALTER TABLE "user" ALTER COLUMN password_hash SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN role_id SET NOT NULL;
