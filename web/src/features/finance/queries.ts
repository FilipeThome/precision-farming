import { useQuery } from '@tanstack/react-query'

import {
  fetchFinanceBudget,
  fetchFinanceCashflow,
  fetchFinanceCosts,
  fetchFinancePnl,
} from './api'

type QueryToggle = { enabled?: boolean }

export const financeKeys = {
  costs: (farmId?: string | null) => ['finance', 'costs', farmId ?? 'all'] as const,
  pnl: (farmId?: string | null) => ['finance', 'pnl', farmId ?? 'all'] as const,
  budget: (farmId?: string | null) => ['finance', 'budget', farmId ?? 'all'] as const,
  cashflow: (farmId?: string | null) => ['finance', 'cashflow', farmId ?? 'all'] as const,
}

export function useFinanceCostsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: financeKeys.costs(farmId),
    queryFn: () => fetchFinanceCosts(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useFinancePnlQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: financeKeys.pnl(farmId),
    queryFn: () => fetchFinancePnl(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useFinanceBudgetQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: financeKeys.budget(farmId),
    queryFn: () => fetchFinanceBudget(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useFinanceCashflowQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: financeKeys.cashflow(farmId),
    queryFn: () => fetchFinanceCashflow(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}
