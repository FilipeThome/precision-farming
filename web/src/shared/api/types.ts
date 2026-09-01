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

export type Season = {
  id: string
  farmId?: string
  name?: string
  crop?: string
  status?: string
  startDate?: string | null
  endDate?: string | null
  year?: number
}

export type ScoutingRecord = {
  id: string
  farmId?: string
  fieldId?: string
  status?: string
  pest?: string
  severity?: string
  notes?: string
  observedAt?: string
}

export type SoilSample = {
  id: string
  farmId?: string
  fieldId?: string
  ph?: number
  organicMatterPct?: number
  status?: string
  sampledAt?: string
  lab?: string
}

export type AgronomyRecommendation = {
  id: string
  farmId?: string
  fieldId?: string
  type?: string
  priority?: string
  status?: string
  summary?: string
  createdAt?: string
}

export type IrrigationAsset = {
  id: string
  farmId?: string
  name?: string
  type?: string
  status?: string
  fieldId?: string
}

export type IrrigationRecommendation = {
  id: string
  farmId?: string
  fieldId?: string
  assetId?: string
  volumeMm?: number
  priority?: string
  status?: string
  recommendedAt?: string
  reason?: string
}

export type HarvestPlan = {
  id: string
  farmId?: string
  fieldId?: string
  crop?: string
  status?: string
  plannedStart?: string | null
  plannedEnd?: string | null
  estimatedTons?: number
}

export type YieldRecord = {
  id: string
  farmId?: string
  fieldId?: string
  crop?: string
  tons?: number
  tonsPerHa?: number
  harvestedAt?: string
}

export type LogisticsLoad = {
  id: string
  farmId?: string
  status?: string
  truckId?: string
  origin?: string
  destination?: string
  tons?: number
  dispatchedAt?: string | null
}

export type StorageUnit = {
  id: string
  farmId?: string
  name?: string
  type?: string
  capacityTons?: number
  occupiedTons?: number
  status?: string
}

export type FinanceCost = {
  id: string
  farmId?: string
  category?: string
  amount?: number
  currency?: string
  period?: string
  fieldId?: string
}

export type FinancePnl = {
  id: string
  farmId?: string
  fieldId?: string
  revenue?: number
  cost?: number
  margin?: number
  currency?: string
  period?: string
}

export type FinanceBudget = {
  id: string
  farmId?: string
  category?: string
  planned?: number
  actual?: number
  currency?: string
  period?: string
}

export type FinanceCashflow = {
  id: string
  farmId?: string
  type?: string
  amount?: number
  currency?: string
  occurredAt?: string
  description?: string
}

export type MarketQuote = {
  id: string
  commodity?: string
  price?: number
  currency?: string
  unit?: string
  quotedAt?: string
  market?: string
}

export type MarketContract = {
  id: string
  farmId?: string
  commodity?: string
  volumeTons?: number
  price?: number
  currency?: string
  status?: string
  counterparty?: string
}

export type MarketExposure = {
  id: string
  farmId?: string
  commodity?: string
  netTons?: number
  markToMarket?: number
  currency?: string
  asOf?: string
}

export type TraceabilityLot = {
  id: string
  farmId?: string
  fieldId?: string
  crop?: string
  lotCode?: string
  status?: string
  harvestedAt?: string
}

export type EsgMetric = {
  id: string
  farmId?: string
  name?: string
  value?: number
  unit?: string
  score?: number
  period?: string
}
