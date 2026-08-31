import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { ackAlert, fetchAlerts } from './api'

export const alertKeys = {
  all: (farmId?: string | null) => ['alerts', farmId ?? 'all'] as const,
}

export function useAlertsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: alertKeys.all(farmId),
    queryFn: () => fetchAlerts(farmId),
    staleTime: 10_000,
  })
}

export function useAckAlertMutation() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ackAlert,
    onSuccess: () => client.invalidateQueries({ queryKey: ['alerts'] }),
  })
}
