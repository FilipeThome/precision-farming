CREATE TABLE maintenance_work_orders (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    machine_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_wo_farm_status ON maintenance_work_orders (farm_id, status);
