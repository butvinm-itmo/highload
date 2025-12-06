CREATE TABLE card (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    arcana_type_id UUID NOT NULL,
    CONSTRAINT fk_card_arcana_type FOREIGN KEY (arcana_type_id)
        REFERENCES arcana_type(id) ON DELETE RESTRICT
);

CREATE INDEX idx_card_arcana_type_id ON card(arcana_type_id);
