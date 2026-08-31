CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    reserved NUMERIC(14,3) NOT NULL DEFAULT 0
);
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES inventory_items(id),
    type VARCHAR(40) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    reference VARCHAR(160)
);
