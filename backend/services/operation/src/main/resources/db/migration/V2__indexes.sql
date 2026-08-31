CREATE INDEX IF NOT EXISTS idx_operations_farm ON operations (farm_id);
CREATE INDEX IF NOT EXISTS idx_operations_status ON operations (status);
CREATE INDEX IF NOT EXISTS idx_saga_operation ON saga_instances (operation_id);
