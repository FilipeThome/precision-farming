export function getGoogleMapsApiKey(): string | undefined {
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  return key && key.trim().length > 0 ? key.trim() : undefined
}

let loadPromise: Promise<typeof google.maps> | null = null

export function loadGoogleMaps(apiKey: string): Promise<typeof google.maps> {
  const existing = window.google?.maps
  if (existing) return Promise.resolve(existing)
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}`
    script.async = true
    script.defer = true
    script.onload = () => {
      if (window.google?.maps) resolve(window.google.maps)
      else reject(new Error('Google Maps não inicializou'))
    }
    script.onerror = () => {
      loadPromise = null
      reject(new Error('Falha ao carregar o Google Maps'))
    }
    document.head.appendChild(script)
  })

  return loadPromise
}

type GeoJsonGeometry = {
  type: string
  coordinates: unknown
}

function ringToPath(ring: number[][]): google.maps.LatLngLiteral[] {
  return ring.map(([lng, lat]) => ({ lat, lng }))
}

export function geometryToPaths(geometryJson: string): google.maps.LatLngLiteral[][] {
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
