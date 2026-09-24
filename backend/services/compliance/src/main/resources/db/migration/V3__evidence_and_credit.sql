CREATE TABLE evidence_packs (
    lot_code VARCHAR(80) PRIMARY KEY,
    farm_id UUID NOT NULL,
    farm_name VARCHAR(200) NOT NULL,
    field_id UUID NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    polygon_geojson TEXT NOT NULL,
    input_refs TEXT NOT NULL,
    receituario_number VARCHAR(80),
    active_ingredient VARCHAR(160),
    moa_group VARCHAR(40),
    responsible_tech_cpf VARCHAR(20),
    phi_days INT,
    deforestation_cutoff_date DATE NOT NULL,
    embargoed BOOLEAN NOT NULL DEFAULT FALSE,
    car_status VARCHAR(40) NOT NULL
);

CREATE TABLE credit_dossiers (
    farm_id UUID PRIMARY KEY,
    car_code VARCHAR(80) NOT NULL,
    car_status VARCHAR(40) NOT NULL,
    embargoed BOOLEAN NOT NULL DEFAULT FALSE,
    deforestation_cutoff_date DATE NOT NULL,
    deforestation_clear BOOLEAN NOT NULL,
    zarc_compliant BOOLEAN NOT NULL,
    remote_sensing_note TEXT
);
