export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export type TokenResponse = {
  accessToken: string
  refreshToken: string
  tokenType?: string
  role: string
  userId: string
  name: string
  email: string
}

export type MeResponse = {
  id: string
  name: string
  email: string
  role: string
}

export type Farm = {
  id: string
  name: string
  location: string
  areaHa: number
  timezone: string
}

export type Field = {
  id: string
  farmId: string
  name: string
  areaHa: number
  crop: string
  variety: string | null
  geometry: string
}

export type Machine = {
  id: string
  farmId: string
  name: string
  type: string
  manufacturer: string
  model: string
  status: string
}

export type Operation = {
  id: string
  fieldId: string
  farmId: string
  type: string
  status: string
  plannedStart: string | null
  plannedEnd: string | null
  actualStart: string | null
  actualEnd: string | null
  machineId: string | null
  pauseReason: string | null
  itemId: string | null
  itemQuantity: number | null
}

export type Alert = {
  id: string
  farmId: string
  severity: string
  type: string
  title: string
  message: string
  entityType: string | null
  entityId: string | null
  status: string
  createdAt: string
}

export type WeatherForecast = {
  id: string
  farmId: string
  forecastAt: string
  temperatureMin: number
  temperatureMax: number
  rainMm: number
  rainProbability: number
  windKmh: number
  humidityPct: number
  sprayingWindow: string
  vintage: string
}

export type InventoryItem = {
  id: string
  farmId: string
  name: string
  category: string
  unit: string
  quantity: number
  reserved: number
}

export type AiInsight = {
  id: string
  type: string
  entityId: string
  score: number
  confidence: number
  horizonHours: number | null
  model: string
  modelVersion: string
  generatedAt: string
  explanation: string[]
  demo: boolean
}

export type ApiErrorBody = {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  correlationId?: string
  details?: Record<string, unknown>
}
