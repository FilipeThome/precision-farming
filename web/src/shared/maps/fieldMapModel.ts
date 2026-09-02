import type { Field, MapLayer } from '@/shared/api/types'
import { geometryToPaths, type LatLng } from '@/shared/maps/geometry'

export type FieldPolygonStyle = {
  id: string
  name: string
  crop: string
  rings: LatLng[][]
  color: string
  fillColor: string
  fillOpacity: number
  weight: number
  dashArray?: string
}

const CROP_COLORS: Record<string, string> = {
  SOY: '#65a30d',
  CORN: '#ca8a04',
  COTTON: '#f8fafc',
}

const LAYER_STYLE: Record<string, { fill: string; stroke: string; dash?: string }> = {
  NDVI: { fill: '#22c55e', stroke: '#15803d' },
  SOIL: { fill: '#a16207', stroke: '#854d0e' },
  YIELD: { fill: '#ea580c', stroke: '#c2410c' },
  WEATHER: { fill: '#38bdf8', stroke: '#0284c7', dash: '6 4' },
}

export function fieldPolygons(
  fields: Field[],
  activeLayerKinds: string[] = [],
  layers: MapLayer[] = [],
): FieldPolygonStyle[] {
  const active = new Set(activeLayerKinds)
  return fields.flatMap((field) => {
    const rings = geometryToPaths(field.geometry ?? '')
    if (rings.length === 0) return []
    const kindsForField = layers
      .filter((layer) => active.has(layer.kind) && (!layer.fieldId || layer.fieldId === field.id))
      .map((layer) => layer.kind)
    const overlay = ['NDVI', 'YIELD', 'SOIL', 'WEATHER'].find((kind) => kindsForField.includes(kind))
    const cropColor = CROP_COLORS[field.crop] ?? '#0d9488'
    const layer = overlay ? LAYER_STYLE[overlay] : undefined
    const outlineOnly = active.size === 0
    return [
      {
        id: field.id,
        name: field.name,
        crop: field.crop,
        rings,
        color: layer?.stroke ?? cropColor,
        fillColor: layer?.fill ?? cropColor,
        fillOpacity: outlineOnly ? 0.12 : layer ? 0.42 : 0.28,
        weight: overlay ? 3 : 2,
        dashArray: layer?.dash,
      },
    ]
  })
}

export function fieldSetKey(fields: Field[]): string {
  return [...new Set(fields.map((field) => field.id))].sort().join(',')
}

export function fitBoundsOf(polygons: FieldPolygonStyle[]): [[number, number], [number, number]] | null {
  const pts = polygons.flatMap((p) => p.rings.flat())
  if (pts.length === 0) return null
  let minLat = pts[0].lat
  let maxLat = pts[0].lat
  let minLng = pts[0].lng
  let maxLng = pts[0].lng
  for (const p of pts) {
    minLat = Math.min(minLat, p.lat)
    maxLat = Math.max(maxLat, p.lat)
    minLng = Math.min(minLng, p.lng)
    maxLng = Math.max(maxLng, p.lng)
  }
  return [
    [minLat, minLng],
    [maxLat, maxLng],
  ]
}
