export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export type { TokenResponse, MeResponse } from '@/features/auth/types'
export type { Farm } from '@/features/farms/types'
export type { Field } from '@/features/fields/types'
export type { Machine, TelemetryPoint, MachineMetrics } from '@/features/machines/types'
export type { Operation, MachineWorkSummary } from '@/features/operations/types'
export type { WorkOrder } from '@/features/maintenance/types'
export type { InventoryItem, InventoryMovement } from '@/features/inventory/types'
export type { Alert } from '@/features/alerts/types'
export type {
  WeatherForecast,
  WeatherWindow,
  ParametricIndex,
  ParametricIndexDay,
  PlantingGate,
} from '@/features/weather/types'
export type { AiInsight } from '@/features/ai/types'
export type { Season } from '@/features/seasons/types'
export type {
  ScoutingRecord,
  SoilSample,
  AgronomyRecommendation,
  PrescriptionMode,
  Prescription,
  SpraySavings,
  MoaRotation,
} from '@/features/agronomy/types'
export type { EvidencePack, CreditDossier, TraceabilityLot, EsgMetric } from '@/features/compliance/types'
export type { MapLayer } from '@/features/map/types'
export type {
  StorageLot,
  HarvestPlan,
  YieldRecord,
  LogisticsLoad,
  StorageUnit,
} from '@/features/harvest/types'
export type { IntegrationConnector } from '@/features/integrations/types'
export type { IrrigationAsset, IrrigationRecommendation } from '@/features/irrigation/types'
export type { FinanceCost, FinancePnl, FinanceBudget, FinanceCashflow } from '@/features/finance/types'
export type { MarketQuote, MarketContract, MarketExposure } from '@/features/market/types'

export type ApiErrorBody = {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  correlationId?: string
  details?: Record<string, unknown>
}
