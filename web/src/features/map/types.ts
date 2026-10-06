export type MapLayer = {
  id: string
  farmId: string
  fieldId: string | null
  name: string
  kind: string
  source: string
  tileUrl: string | null
  acquiredAt: string | null
  status: string
}
