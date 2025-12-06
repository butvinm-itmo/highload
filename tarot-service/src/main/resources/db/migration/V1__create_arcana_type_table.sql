CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE arcana_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(16) NOT NULL
);
