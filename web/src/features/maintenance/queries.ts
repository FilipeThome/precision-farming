import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { createWorkOrder, fetchWorkOrders } from './api'

export const maintenanceKeys = {
  workOrders: (farmId?: string | null) => ['maintenance', 'work-orders', farmId ?? 'all'] as const,
}

export function useWorkOrdersQuery(farmId?: string | null) {
  return useQuery({
    queryKey: maintenanceKeys.workOrders(farmId),
    queryFn: () => fetchWorkOrders(farmId),
    staleTime: 30_000,
  })
}

export function useWorkOrderCommands() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: createWorkOrder,
    onSuccess: () => void client.invalidateQueries({ queryKey: ['maintenance', 'work-orders'] }),
  })
}
