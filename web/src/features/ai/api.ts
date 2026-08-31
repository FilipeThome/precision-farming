import { apiGet } from '@/shared/api/client'
import type { AiInsight } from '@/shared/api/types'

export async function fetchInsights(farmId?: string | null): Promise<AiInsight[]> {
  return apiGet<AiInsight[]>('/api/v1/ai/insights', { farmId: farmId ?? undefined })
}

export async function fetchMachineRisk(machineId: string): Promise<AiInsight[]> {
  return apiGet<AiInsight[]>('/api/v1/ai/machine-risk', { machineId })
}
