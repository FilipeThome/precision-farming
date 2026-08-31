CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
