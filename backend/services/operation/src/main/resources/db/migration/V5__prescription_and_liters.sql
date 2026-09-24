ALTER TABLE operations ADD COLUMN prescription_id UUID;
ALTER TABLE operations ADD COLUMN actual_liters NUMERIC(14,4);
CREATE INDEX IF NOT EXISTS idx_operations_prescription ON operations (prescription_id);
