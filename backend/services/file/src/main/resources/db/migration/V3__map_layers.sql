CREATE TABLE map_layers (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID,
    name VARCHAR(160) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    source VARCHAR(80) NOT NULL,
    tile_url VARCHAR(255),
    acquired_at TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_map_layers_farm ON map_layers (farm_id, kind);
