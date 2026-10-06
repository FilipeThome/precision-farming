export type StorageLot = {
  id: string
  unitId: string
  farmId: string
  crop: string
  tons: number
  quality: string
  receivedAt: string
}

export type HarvestPlan = {
  id: string
  farmId?: string
  fieldId?: string
  crop?: string
  status?: string
  plannedStart?: string | null
  plannedEnd?: string | null
  expectedTHa?: number
}

export type YieldRecord = {
  id: string
  farmId?: string
  fieldId?: string
  fieldName?: string
  planId?: string | null
  recordedAt?: string
  yieldTHa?: number
  moisturePct?: number | null
  areaHa?: number | null
}

export type LogisticsLoad = {
  id: string
  farmId?: string
  planId?: string | null
  truckPlate?: string
  destination?: string
  tons?: number
  status?: string
  dispatchedAt?: string | null
}

export type StorageUnit = {
  id: string
  farmId?: string
  name?: string
  type?: string
  capacityT?: number
  usedT?: number
}
