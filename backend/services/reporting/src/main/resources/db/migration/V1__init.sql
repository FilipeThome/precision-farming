CREATE TABLE report_jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
