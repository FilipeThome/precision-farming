import { useQuery } from '@tanstack/react-query'

import { fetchMachines } from './api'

export const machineKeys = {
  all: (farmId?: string | null) => ['machines', farmId ?? 'all'] as const,
}

export function useMachinesQuery(farmId?: string | null) {
  return useQuery({
    queryKey: machineKeys.all(farmId),
    queryFn: () => fetchMachines(farmId),
    staleTime: 30_000,
  })
}
