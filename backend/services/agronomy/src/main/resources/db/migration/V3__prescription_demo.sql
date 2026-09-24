ALTER TABLE prescriptions ADD COLUMN mode VARCHAR(40) NOT NULL DEFAULT 'BROADCAST';
ALTER TABLE prescriptions ADD COLUMN treated_fraction NUMERIC(6,4) NOT NULL DEFAULT 1;
ALTER TABLE prescriptions ADD COLUMN active_ingredient VARCHAR(160);
ALTER TABLE prescriptions ADD COLUMN moa_group VARCHAR(40);
ALTER TABLE prescriptions ADD COLUMN receituario_number VARCHAR(80);
ALTER TABLE prescriptions ADD COLUMN responsible_tech_cpf VARCHAR(20);
ALTER TABLE prescriptions ADD COLUMN phi_days INT;
ALTER TABLE prescriptions ADD COLUMN reentry_hours INT;
ALTER TABLE prescriptions ADD COLUMN field_area_ha NUMERIC(12,4);

UPDATE prescriptions SET active_ingredient = product WHERE active_ingredient IS NULL;
