CREATE TABLE weather_windows (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    window_type VARCHAR(40) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    rating VARCHAR(40) NOT NULL,
    notes VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_weather_windows_farm ON weather_windows (farm_id, window_type);
