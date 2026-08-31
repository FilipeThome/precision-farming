CREATE TABLE machines (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(80) NOT NULL,
    manufacturer VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL
);
