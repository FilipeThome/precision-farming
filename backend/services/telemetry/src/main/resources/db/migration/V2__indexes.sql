CREATE INDEX IF NOT EXISTS idx_telemetry_machine_observed
    ON telemetry_observations (machine_id, observed_at);
