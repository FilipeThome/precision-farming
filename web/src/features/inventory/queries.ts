import { useQuery } from '@tanstack/react-query'

import { fetchInventory } from './api'

export const inventoryKeys = {
  all: (farmId?: string | null) => ['inventory', farmId ?? 'all'] as const,
}

export function useInventoryQuery(farmId?: string | null) {
  return useQuery({
    queryKey: inventoryKeys.all(farmId),
    queryFn: () => fetchInventory(farmId),
    staleTime: 30_000,
  })
}
