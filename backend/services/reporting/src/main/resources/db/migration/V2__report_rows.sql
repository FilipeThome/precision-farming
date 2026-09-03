CREATE TABLE report_operation_rows (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL
);
CREATE INDEX idx_report_operation_farm ON report_operation_rows (farm_id);

CREATE TABLE report_inventory_rows (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    quantity NUMERIC(14, 2) NOT NULL
);
CREATE INDEX idx_report_inventory_farm ON report_inventory_rows (farm_id);
