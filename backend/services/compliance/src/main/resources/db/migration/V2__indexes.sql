CREATE INDEX IF NOT EXISTS idx_trace_farm ON traceability_records (farm_id);
CREATE INDEX IF NOT EXISTS idx_esg_farm ON esg_metrics (farm_id, metric);
