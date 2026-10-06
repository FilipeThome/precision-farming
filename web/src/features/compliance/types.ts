export type EvidencePack = {
  lotCode: string
  farmId: string
  farmName: string
  fieldId: string
  fieldName: string
  polygonGeoJson: unknown
  inputRefs: unknown
  receituarioNumber: string | null
  activeIngredient: string | null
  moaGroup: string | null
  responsibleTechCpf: string | null
  phiDays: number | null
  deforestationCutoffDate: string | null
  embargoed: boolean
  carStatus: string | null
  simulation?: boolean
}

export type CreditDossier = {
  farmId: string
  carCode: string | null
  carStatus: string | null
  embargoed: boolean
  deforestationCutoffDate: string | null
  deforestationClear: boolean
  zarcCompliant: boolean
  remoteSensingNote: string | null
  simulation?: boolean
}

export type TraceabilityLot = {
  id: string
  farmId: string
  fieldId: string | null
  lotCode: string
  crop: string
  eventType: string
  summary: string
  occurredAt: string
}

export type EsgMetric = {
  id: string
  farmId?: string
  metric?: string
  value?: number
  unit?: string
  score?: number | null
  periodLabel?: string
}
