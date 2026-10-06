import { apiGet } from '@/shared/api/client'
import type { FinanceBudget, FinanceCashflow, FinanceCost, FinancePnl } from '@/features/finance/types'

export async function fetchFinanceCosts(farmId?: string | null): Promise<FinanceCost[]> {
  return apiGet<FinanceCost[]>('/api/v1/finance/costs', { farmId: farmId ?? undefined })
}

export async function fetchFinancePnl(farmId?: string | null): Promise<FinancePnl[]> {
  return apiGet<FinancePnl[]>('/api/v1/finance/pnl', { farmId: farmId ?? undefined })
}

export async function fetchFinanceBudget(farmId?: string | null): Promise<FinanceBudget[]> {
  return apiGet<FinanceBudget[]>('/api/v1/finance/budget', { farmId: farmId ?? undefined })
}

export async function fetchFinanceCashflow(farmId?: string | null): Promise<FinanceCashflow[]> {
  return apiGet<FinanceCashflow[]>('/api/v1/finance/cashflow', { farmId: farmId ?? undefined })
}
