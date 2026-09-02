CREATE TABLE scouting_observations (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    pest VARCHAR(120),
    severity VARCHAR(40) NOT NULL,
    notes TEXT,
    status VARCHAR(40) NOT NULL
);
CREATE TABLE soil_samples (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    sampled_at TIMESTAMPTZ NOT NULL,
    ph NUMERIC(4,2),
    organic_matter_pct NUMERIC(6,2),
    p_ppm NUMERIC(8,2),
    k_ppm NUMERIC(8,2),
    lab_ref VARCHAR(120),
    status VARCHAR(40) NOT NULL
);
CREATE TABLE recommendations (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID,
    kind VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    priority VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID NOT NULL,
    product VARCHAR(160) NOT NULL,
    rate NUMERIC(10,2) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ
);
