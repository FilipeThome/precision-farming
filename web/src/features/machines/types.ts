export type Machine = {
  id: string
  farmId: string
  name: string
  type: string
  manufacturer: string
  model: string
  status: string
  photoFileId?: string | null
  photoUrl?: string | null
}

export type TelemetryPoint = {
  id: string
  machineId: string
  observedAt: string
  lat: number
  lon: number
  speedKmh: number
  rpm: number
  fuelPct: number
  engineTempC: number
}

export type MachineMetrics = {
  engineHours: number
  lastObservedAt: string | null
  days: Array<{ day: string; hours: number; speed: number; fuel: number }>
}
