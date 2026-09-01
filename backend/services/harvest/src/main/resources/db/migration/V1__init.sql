CREATE TABLE harvest_plans (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    crop VARCHAR(80) NOT NULL,
    planned_start TIMESTAMPTZ NOT NULL,
    planned_end TIMESTAMPTZ NOT NULL,
    expected_t_ha NUMERIC(8,2) NOT NULL,
    status VARCHAR(40) NOT NULL
);
CREATE TABLE harvest_yields (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    plan_id UUID,
    recorded_at TIMESTAMPTZ NOT NULL,
    yield_t_ha NUMERIC(8,2) NOT NULL,
    moisture_pct NUMERIC(6,2),
    area_ha NUMERIC(10,2)
);
CREATE TABLE logistics_loads (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    plan_id UUID,
    truck_plate VARCHAR(40) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    tons NUMERIC(10,2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    dispatched_at TIMESTAMPTZ
);
CREATE TABLE storage_units (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    capacity_t NUMERIC(12,2) NOT NULL,
    used_t NUMERIC(12,2) NOT NULL,
    type VARCHAR(80) NOT NULL
);
CREATE TABLE storage_lots (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL,
    farm_id UUID NOT NULL,
    crop VARCHAR(80) NOT NULL,
    tons NUMERIC(12,2) NOT NULL,
    quality VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);
