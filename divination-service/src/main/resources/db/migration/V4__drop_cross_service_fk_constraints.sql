-- Drop cross-service FK constraints to enable independent database deployment
-- Application-level validation via Feign clients is already in place

-- spread table: drop FK to user.id and layout_type.id
ALTER TABLE spread DROP CONSTRAINT IF EXISTS fk_spread_author;
ALTER TABLE spread DROP CONSTRAINT IF EXISTS fk_spread_layout_type;

-- spread_card table: drop FK to card.id
ALTER TABLE spread_card DROP CONSTRAINT IF EXISTS fk_spread_card_card;

-- interpretation table: drop FK to user.id
ALTER TABLE interpretation DROP CONSTRAINT IF EXISTS fk_interpretation_author;

-- Note: Indexes (idx_spread_author_id, idx_spread_card_card_id, idx_interpretation_author_id)
-- are preserved for query performance
