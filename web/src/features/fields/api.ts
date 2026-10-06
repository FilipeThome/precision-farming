import { apiGet, apiPatch, apiPost } from '@/shared/api/client'
import type { Field } from '@/features/fields/types'

export type FieldBody = {
  farmId: string
  name: string
  areaHa: number
  crop: string
  variety: string | null
  geometry: string
}

export async function fetchFields(farmId?: string | null): Promise<Field[]> {
  return apiGet<Field[]>('/api/v1/fields', { farmId: farmId ?? undefined })
}

export async function createField(body: FieldBody): Promise<Field> {
  return apiPost<Field>('/api/v1/fields', body)
}

export async function patchField(id: string, body: FieldBody): Promise<Field> {
  return apiPatch<Field>(`/api/v1/fields/${id}`, body)
}
