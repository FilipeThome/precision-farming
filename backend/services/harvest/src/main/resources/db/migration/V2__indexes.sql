CREATE INDEX IF NOT EXISTS idx_harvest_plans_farm ON harvest_plans (farm_id);
CREATE INDEX IF NOT EXISTS idx_loads_farm ON logistics_loads (farm_id, status);
CREATE INDEX IF NOT EXISTS idx_storage_lots_unit ON storage_lots (unit_id);
