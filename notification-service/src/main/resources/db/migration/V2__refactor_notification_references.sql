-- Add new specific columns for notification references
ALTER TABLE notification ADD COLUMN spread_id UUID NULL;
ALTER TABLE notification ADD COLUMN interpretation_id UUID NULL;

-- Migrate existing NEW_INTERPRETATION notifications
-- (interpretation_id = old reference_id, spread_id cannot be backfilled without cross-service query)
UPDATE notification
SET interpretation_id = reference_id
WHERE type = 'NEW_INTERPRETATION';

-- Drop old generic reference columns
ALTER TABLE notification DROP COLUMN reference_id;
ALTER TABLE notification DROP COLUMN reference_type;
