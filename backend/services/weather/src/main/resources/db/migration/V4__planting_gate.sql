CREATE TABLE planting_gate_configs (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    municipality VARCHAR(160) NOT NULL,
    crop VARCHAR(40) NOT NULL,
    zarc_start_month INT NOT NULL,
    zarc_start_day INT NOT NULL,
    zarc_end_month INT NOT NULL,
    zarc_end_day INT NOT NULL,
    void_start_month INT NOT NULL,
    void_start_day INT NOT NULL,
    void_end_month INT NOT NULL,
    void_end_day INT NOT NULL
);
CREATE UNIQUE INDEX idx_planting_gate_farm_crop ON planting_gate_configs (farm_id, crop);
