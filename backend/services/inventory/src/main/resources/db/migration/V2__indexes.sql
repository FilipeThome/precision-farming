CREATE INDEX IF NOT EXISTS idx_inventory_items_farm ON inventory_items (farm_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_item ON inventory_movements (item_id);
