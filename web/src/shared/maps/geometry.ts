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

/** Axis-aligned square GeoJSON Polygon around a WGS84 point. */
export function squarePolygon(lat: number, lon: number, areaHa: number): string {
  const areaM2 = Math.max(areaHa, 0.0001) * 10_000
  const halfM = Math.sqrt(areaM2) / 2
  const dLat = halfM / 111_320
  const cos = Math.cos((lat * Math.PI) / 180)
  const dLon = halfM / (111_320 * Math.max(Math.abs(cos), 0.01))
  const ring = [
    [lon - dLon, lat - dLat],
    [lon + dLon, lat - dLat],
    [lon + dLon, lat + dLat],
    [lon - dLon, lat + dLat],
    [lon - dLon, lat - dLat],
  ]
  return JSON.stringify({ type: 'Polygon', coordinates: [ring] })
}

export function centroidOfGeometry(geometry: string): { lat: number; lon: number } | null {
  try {
    const parsed = JSON.parse(geometry) as GeoJsonGeometry
    const ring = firstRing(parsed)
    if (!ring || ring.length < 3) return null
    let lon = 0
    let lat = 0
    const n = ring.length - 1
    for (let i = 0; i < n; i += 1) {
      lon += ring[i][0]
      lat += ring[i][1]
    }
    return { lon: lon / n, lat: lat / n }
  } catch {
    return null
  }
}

function firstRing(parsed: GeoJsonGeometry): number[][] | null {
  const coords = parsed.coordinates
  if (!Array.isArray(coords) || coords.length === 0) return null
  if (parsed.type === 'Polygon') return coords[0] as number[][]
  if (parsed.type === 'MultiPolygon') {
    const first = coords[0] as unknown[]
    return Array.isArray(first) ? (first[0] as number[][]) : null
  }
  return null
}

/** Keep the stored polygon unless the operator changed lat, lon, or area. */
export function geometryForFieldSave(input: {
  existingGeometry?: string | null
  originalLat: string
  originalLon: string
  originalAreaHa: string
  lat: string
  lon: string
  areaHa: string
}): string {
  if (
    input.existingGeometry &&
    input.lat === input.originalLat &&
    input.lon === input.originalLon &&
    input.areaHa === input.originalAreaHa
  ) {
    return input.existingGeometry
  }
  return squarePolygon(Number(input.lat), Number(input.lon), Number(input.areaHa))
}
