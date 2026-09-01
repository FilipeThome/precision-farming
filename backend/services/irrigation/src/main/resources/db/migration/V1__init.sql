CREATE TABLE irrigation_assets (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    capacity_mm_h NUMERIC(8,2)
);
CREATE TABLE irrigation_recommendations (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    asset_id UUID,
    recommended_mm NUMERIC(8,2) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL
);
CREATE TABLE irrigation_simulations (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    mm NUMERIC(8,2) NOT NULL,
    duration_h NUMERIC(8,2) NOT NULL,
    estimated_cost NUMERIC(12,2) NOT NULL,
    water_m3 NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
