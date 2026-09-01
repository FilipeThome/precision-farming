import { useQuery } from '@tanstack/react-query'

import { fetchIntegrations } from './api'

export const integrationKeys = {
  all: ['integrations'] as const,
}

export function useIntegrationsQuery() {
  return useQuery({
    queryKey: integrationKeys.all,
    queryFn: fetchIntegrations,
    staleTime: 60_000,
  })
}
