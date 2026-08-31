import { apiGet } from '@/shared/api/client'
import type { Field } from '@/shared/api/types'

export async function fetchFields(farmId?: string | null): Promise<Field[]> {
  return apiGet<Field[]>('/api/v1/fields', { farmId: farmId ?? undefined })
}
