CREATE TABLE operations (
    id UUID PRIMARY KEY,
    field_id UUID NOT NULL,
    farm_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    planned_start TIMESTAMPTZ,
    planned_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ,
    machine_id UUID,
    pause_reason VARCHAR(255),
    item_id UUID,
    item_quantity NUMERIC(12,3)
);
CREATE TABLE saga_instances (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    state VARCHAR(40) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
