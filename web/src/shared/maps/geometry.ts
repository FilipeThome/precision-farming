export type LatLng = { lat: number; lng: number }

type GeoJsonGeometry = {
  type: string
  coordinates: unknown
}

function ringToPath(ring: number[][]): LatLng[] {
  return ring.map(([lng, lat]) => ({ lat, lng }))
}

/** Parse GeoJSON Polygon / MultiPolygon into Leaflet-ready lat/lng rings. */
export function geometryToPaths(geometryJson: string): LatLng[][] {
  try {
    const geom = JSON.parse(geometryJson) as GeoJsonGeometry
    if (geom.type === 'Polygon') {
      return (geom.coordinates as number[][][]).map(ringToPath)
    }
    if (geom.type === 'MultiPolygon') {
      return (geom.coordinates as number[][][][]).flatMap((polygon) => polygon.map(ringToPath))
    }
  } catch {
    return []
  }
  return []
}

export function shortId(id?: string | null): string {
  if (!id) return '—'
  return id.length > 8 ? id.slice(0, 8) : id
}
