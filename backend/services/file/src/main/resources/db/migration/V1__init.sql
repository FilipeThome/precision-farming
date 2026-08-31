CREATE TABLE files (
    id UUID PRIMARY KEY,
    farm_id UUID,
    field_id UUID,
    kind VARCHAR(40) NOT NULL,
    source VARCHAR(80) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    acquisition_at TIMESTAMPTZ,
    processing_version VARCHAR(40),
    quality VARCHAR(40)
);
