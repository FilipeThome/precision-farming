export type IrrigationAsset = {
  id: string
  farmId?: string
  name?: string
  type?: string
  status?: string
  fieldId?: string
  capacityMmH?: number | null
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
