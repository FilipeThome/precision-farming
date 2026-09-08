import { useQuery } from '@tanstack/react-query'

import { fetchInventory, fetchInventoryMovements } from './api'

export const inventoryKeys = {
  all: (farmId?: string | null) => ['inventory', farmId ?? 'all'] as const,
  movements: (itemId: string) => ['inventory', itemId, 'movements'] as const,
}

export function useInventoryQuery(farmId?: string | null) {
  return useQuery({
    queryKey: inventoryKeys.all(farmId),
    queryFn: () => fetchInventory(farmId),
    staleTime: 30_000,
  })
}

export function useInventoryMovementsQuery(itemId?: string | null) {
  return useQuery({
    queryKey: inventoryKeys.movements(itemId ?? ''),
    queryFn: () => fetchInventoryMovements(itemId!),
    enabled: Boolean(itemId),
    staleTime: 30_000,
  })
}
