CREATE TABLE connectors (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(80) NOT NULL,
    mode VARCHAR(20) NOT NULL
);
