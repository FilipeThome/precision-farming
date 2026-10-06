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
  kind?: string
  title?: string
  priority?: string
  status?: string
  summary?: string
  createdAt?: string
}

export type PrescriptionMode = 'BROADCAST' | 'SPOT'

export type Prescription = {
  id: string
  farmId: string
  fieldId: string
  product: string
  mode?: PrescriptionMode | string | null
  treatedFraction?: number | null
  plannedDose: number
  unit: string
  activeIngredient?: string | null
  moaGroup?: string | null
  receituarioNumber?: string | null
  responsibleTechCpf?: string | null
  phiDays?: number | null
  reentryHours?: number | null
  fieldAreaHa?: number | null
  status: string
  createdAt: string
  approvedAt: string | null
}

export type SpraySavings = {
  prescriptionId: string
  fieldId: string
  mode: string
  fieldAreaHa: number
  treatedHa: number
  fullRateHa: number
  litersFullRate: number
  litersSpot: number
  litersAvoided: number
  litersPerHa: number
  unit: string
  simulation?: boolean
}

export type MoaRotation = {
  fieldId: string
  moaGroup: string
  warning: boolean
  prescriptionIds: string[]
  simulation?: boolean
}
