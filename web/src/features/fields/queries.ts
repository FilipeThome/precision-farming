import { useQuery } from '@tanstack/react-query'

import { fetchFields } from './api'

export const fieldKeys = {
  all: (farmId?: string | null) => ['fields', farmId ?? 'all'] as const,
}

export function useFieldsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: fieldKeys.all(farmId),
    queryFn: () => fetchFields(farmId),
    staleTime: 30_000,
  })
}
