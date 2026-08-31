CREATE TABLE telemetry_observations (
    id UUID NOT NULL,
    machine_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION,
    rpm DOUBLE PRECISION,
    fuel_pct DOUBLE PRECISION,
    engine_temp_c DOUBLE PRECISION,
    PRIMARY KEY (id, observed_at)
);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
    PERFORM create_hypertable('telemetry_observations', 'observed_at', if_not_exists => TRUE);
  END IF;
END$$;
