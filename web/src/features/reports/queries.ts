import { useQuery } from '@tanstack/react-query'

import { fetchReportsCatalog } from './api'

export const reportKeys = {
  catalog: (farmId?: string | null) => ['reports', 'catalog', farmId ?? 'all'] as const,
}

export function useReportsCatalogQuery(farmId?: string | null) {
  return useQuery({
    queryKey: reportKeys.catalog(farmId),
    queryFn: fetchReportsCatalog,
    staleTime: 30_000,
  })
}
