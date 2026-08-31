CREATE INDEX IF NOT EXISTS idx_fields_farm_id ON fields (farm_id);
CREATE INDEX IF NOT EXISTS idx_fields_geometry ON fields USING GIST (geometry);
