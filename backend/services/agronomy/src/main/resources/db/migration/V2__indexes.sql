CREATE INDEX IF NOT EXISTS idx_scouting_farm ON scouting_observations (farm_id);
CREATE INDEX IF NOT EXISTS idx_soil_farm ON soil_samples (farm_id);
CREATE INDEX IF NOT EXISTS idx_reco_farm ON recommendations (farm_id);
CREATE INDEX IF NOT EXISTS idx_rx_farm ON prescriptions (farm_id, status);
