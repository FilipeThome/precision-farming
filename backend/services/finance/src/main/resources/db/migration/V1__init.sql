CREATE TABLE cost_transactions (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    field_id UUID,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    season_label VARCHAR(80) NOT NULL,
    category VARCHAR(80) NOT NULL,
    planned NUMERIC(14,2) NOT NULL,
    actual NUMERIC(14,2) NOT NULL
);
CREATE TABLE cashflow_entries (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    label VARCHAR(160) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE market_quotes (
    id UUID PRIMARY KEY,
    commodity VARCHAR(80) NOT NULL,
    exchange VARCHAR(80) NOT NULL,
    price NUMERIC(14,4) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    quoted_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE market_contracts (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    commodity VARCHAR(80) NOT NULL,
    volume_t NUMERIC(12,2) NOT NULL,
    price NUMERIC(14,4) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    delivery_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL
);
CREATE TABLE market_exposures (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    commodity VARCHAR(80) NOT NULL,
    open_t NUMERIC(12,2) NOT NULL,
    hedged_t NUMERIC(12,2) NOT NULL,
    risk_score NUMERIC(6,2) NOT NULL
);
