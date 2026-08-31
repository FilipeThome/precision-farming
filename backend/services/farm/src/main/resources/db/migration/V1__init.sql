CREATE EXTENSION IF NOT EXISTS postgis;
CREATE TABLE farms (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    location VARCHAR(160) NOT NULL,
    area_ha NUMERIC(12,4) NOT NULL,
    timezone VARCHAR(64) NOT NULL
);
CREATE TABLE fields (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    name VARCHAR(160) NOT NULL,
    area_ha NUMERIC(12,4) NOT NULL,
    crop VARCHAR(80) NOT NULL,
    variety VARCHAR(80),
    geometry geometry(MultiPolygon, 4326) NOT NULL,
    centroid geometry(Point, 4326)
);
