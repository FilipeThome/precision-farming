export type FinanceCost = {
  id: string
  farmId?: string
  category?: string
  description?: string
  amount?: number
  currency?: string
  occurredAt?: string
  fieldId?: string
}

export type FinancePnl = {
  id: string
  farmId?: string
  farmName?: string
  fieldId?: string
  revenue?: number
  cost?: number
  margin?: number
  currency?: string
  period?: string
}

export type FinanceBudget = {
  id: string
  farmId?: string
  category?: string
  seasonLabel?: string
  planned?: number
  actual?: number
  currency?: string
}

export type FinanceCashflow = {
  id: string
  farmId?: string
  label?: string
  direction?: string
  amount?: number
  dueAt?: string
}
