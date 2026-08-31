CREATE TABLE predictions (
    id UUID PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    score NUMERIC(6,4) NOT NULL,
    confidence NUMERIC(6,4) NOT NULL,
    model VARCHAR(80) NOT NULL,
    model_version VARCHAR(40) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    explanation TEXT NOT NULL,
    horizon_hours INT
);
