CREATE INDEX IF NOT EXISTS idx_weather_farm_forecast
    ON weather_forecasts (farm_id, forecast_at);
