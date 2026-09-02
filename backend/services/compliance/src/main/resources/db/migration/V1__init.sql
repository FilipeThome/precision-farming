CREATE TABLE traceability_records (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID,
    lot_code VARCHAR(80) NOT NULL,
    crop VARCHAR(80) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    summary TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE esg_metrics (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    metric VARCHAR(80) NOT NULL,
    value NUMERIC(14,4) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    period_label VARCHAR(40) NOT NULL,
    score NUMERIC(6,2)
);
