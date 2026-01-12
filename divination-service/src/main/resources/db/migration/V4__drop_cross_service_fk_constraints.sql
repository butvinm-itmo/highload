-- Drop cross-service foreign key constraints
-- These constraints reference tables owned by other microservices (user-service, tarot-service)
-- Data integrity is now enforced at the application level via Feign client validation
-- Cascade delete for user deletion is handled via synchronous API call from user-service

-- Drop FK from spread to layout_type (tarot-service)
ALTER TABLE spread DROP CONSTRAINT IF EXISTS fk_spread_layout_type;

-- Drop FK from spread to user (user-service)
ALTER TABLE spread DROP CONSTRAINT IF EXISTS fk_spread_author;

-- Drop FK from spread_card to card (tarot-service)
ALTER TABLE spread_card DROP CONSTRAINT IF EXISTS fk_spread_card_card;

-- Drop FK from interpretation to user (user-service)
ALTER TABLE interpretation DROP CONSTRAINT IF EXISTS fk_interpretation_author;

-- Note: Internal FKs are preserved:
-- - fk_spread_card_spread (spread_card.spread_id -> spread.id) with ON DELETE CASCADE
-- - fk_interpretation_spread (interpretation.spread_id -> spread.id) with ON DELETE CASCADE
