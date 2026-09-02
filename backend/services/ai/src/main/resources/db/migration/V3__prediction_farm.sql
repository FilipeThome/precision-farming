ALTER TABLE predictions ADD COLUMN IF NOT EXISTS farm_id UUID;
CREATE INDEX IF NOT EXISTS idx_predictions_farm ON predictions (farm_id);
