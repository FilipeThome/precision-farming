import { useQuery } from '@tanstack/react-query'

import { fetchMarketContracts, fetchMarketExposure, fetchMarketQuotes } from './api'

type QueryToggle = { enabled?: boolean }

export const marketKeys = {
  quotes: (farmId?: string | null) => ['market', 'quotes', farmId ?? 'all'] as const,
  contracts: (farmId?: string | null) => ['market', 'contracts', farmId ?? 'all'] as const,
  exposure: (farmId?: string | null) => ['market', 'exposure', farmId ?? 'all'] as const,
}

export function useMarketQuotesQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: marketKeys.quotes(farmId),
    queryFn: () => fetchMarketQuotes(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useMarketContractsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: marketKeys.contracts(farmId),
    queryFn: () => fetchMarketContracts(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useMarketExposureQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: marketKeys.exposure(farmId),
    queryFn: () => fetchMarketExposure(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}
