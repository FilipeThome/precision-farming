CREATE INDEX IF NOT EXISTS idx_costs_farm ON cost_transactions (farm_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_budget_farm ON budgets (farm_id);
CREATE INDEX IF NOT EXISTS idx_contracts_farm ON market_contracts (farm_id);
