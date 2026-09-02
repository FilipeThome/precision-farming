CREATE TABLE seasons (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    name VARCHAR(120) NOT NULL,
    crop VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(40) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_seasons_farm ON seasons (farm_id);
