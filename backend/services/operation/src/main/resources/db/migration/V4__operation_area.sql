ALTER TABLE operations ADD COLUMN area_ha NUMERIC(12,4);
CREATE INDEX IF NOT EXISTS idx_operations_machine ON operations (machine_id);
