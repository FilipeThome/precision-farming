import { apiGet, apiPatch, apiPost, apiUpload } from '@/shared/api/client'
import type { Machine, MachineMetrics, TelemetryPoint } from '@/shared/api/types'

export type MachineBody = {
  farmId: string
  name: string
  type: string
  manufacturer: string
  model: string
  status: string
  photoFileId?: string | null
}

export async function fetchMachines(farmId?: string | null): Promise<Machine[]> {
  return apiGet<Machine[]>('/api/v1/machines', { farmId: farmId ?? undefined })
}

export async function fetchMachineTelemetry(machineId: string): Promise<TelemetryPoint[]> {
  return apiGet<TelemetryPoint[]>(`/api/v1/machines/${machineId}/telemetry`)
}

export async function fetchMachineMetrics(machineId: string): Promise<MachineMetrics> {
  return apiGet<MachineMetrics>(`/api/v1/machines/${machineId}/metrics`)
}

export async function createMachine(body: MachineBody): Promise<Machine> {
  return apiPost<Machine>('/api/v1/machines', body)
}

export async function patchMachine(id: string, body: MachineBody): Promise<Machine> {
  return apiPatch<Machine>(`/api/v1/machines/${id}`, body)
}

export async function saveMachine(input: {
  id?: string
  body: MachineBody
  photo?: File | null
}): Promise<Machine> {
  let photoFileId = input.body.photoFileId ?? null
  if (input.photo) {
    const form = new FormData()
    form.append('farmId', input.body.farmId)
    form.append('kind', 'MACHINE_PHOTO')
    if (input.id) form.append('entityId', input.id)
    form.append('file', input.photo)
    const meta = await apiUpload<{ id: string }>('/api/v1/files', form)
    photoFileId = meta.id
  }
  const body = { ...input.body, photoFileId }
  return input.id ? patchMachine(input.id, body) : createMachine(body)
}
