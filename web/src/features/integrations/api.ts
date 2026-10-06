import { apiGet } from '@/shared/api/client'
import type { IntegrationConnector } from '@/features/integrations/types'

export async function fetchIntegrations(): Promise<IntegrationConnector[]> {
  return apiGet<IntegrationConnector[]>('/api/v1/integrations')
}
