CREATE TABLE weather_forecasts (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    forecast_at TIMESTAMPTZ NOT NULL,
    temperature_min NUMERIC(6,2),
    temperature_max NUMERIC(6,2),
    rain_mm NUMERIC(8,2),
    rain_probability NUMERIC(5,2),
    wind_kmh NUMERIC(6,2),
    humidity_pct NUMERIC(5,2),
    spraying_window VARCHAR(32),
    vintage VARCHAR(32)
);
