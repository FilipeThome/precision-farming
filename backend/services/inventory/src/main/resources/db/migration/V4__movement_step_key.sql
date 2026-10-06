ALTER TABLE inventory_movements ADD COLUMN step_key VARCHAR(160);
CREATE UNIQUE INDEX ux_inventory_movement_step
    ON inventory_movements (item_id, type, step_key)
    WHERE step_key IS NOT NULL;
