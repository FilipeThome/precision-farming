import { useEffect, useRef, useState } from 'react'
import { MapPinOff } from 'lucide-react'

import type { Field } from '@/shared/api/types'
import { Card } from '@/shared/ui/Card'

import { geometryToPaths, getGoogleMapsApiKey, loadGoogleMaps } from './loadGoogleMaps'

type FieldMapProps = {
  fields: Field[]
  className?: string
}

function fieldsSignature(fields: Field[]): string {
  return fields.map((field) => `${field.id}:${field.geometry}`).join('|')
}

export function FieldMap({ fields, className = 'h-[520px]' }: FieldMapProps) {
  const apiKey = getGoogleMapsApiKey()
  const containerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<google.maps.Map | null>(null)
  const mapsRef = useRef<typeof google.maps | null>(null)
  const polygonsRef = useRef<google.maps.Polygon[]>([])
  const fieldsRef = useRef(fields)
  fieldsRef.current = fields
  const [mapReady, setMapReady] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const signature = fieldsSignature(fields)

  useEffect(() => {
    if (!apiKey || !containerRef.current) return
    const node = containerRef.current
    let cancelled = false

    loadGoogleMaps(apiKey)
      .then((maps) => {
        if (cancelled || !node) return
        mapsRef.current = maps
        const map = new maps.Map(node, {
          mapTypeId: maps.MapTypeId.SATELLITE,
          tilt: 0,
          streetViewControl: false,
          fullscreenControl: true,
        })
        map.setCenter({ lat: -19.0, lng: -54.5 })
        mapRef.current = map
        setMapReady(true)
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : 'Falha ao carregar o mapa')
      })

    return () => {
      cancelled = true
      polygonsRef.current.forEach((polygon) => polygon.setMap(null))
      polygonsRef.current = []
      mapRef.current = null
      mapsRef.current = null
      setMapReady(false)
    }
  }, [apiKey])

  useEffect(() => {
    const map = mapRef.current
    const maps = mapsRef.current
    if (!mapReady || !map || !maps) return

    polygonsRef.current.forEach((polygon) => polygon.setMap(null))
    polygonsRef.current = []

    const bounds = new maps.LatLngBounds()
    let hasPath = false
    for (const field of fieldsRef.current) {
      for (const path of geometryToPaths(field.geometry)) {
        if (path.length === 0) continue
        hasPath = true
        polygonsRef.current.push(
          new maps.Polygon({
            map,
            paths: path,
            strokeColor: '#0D9488',
            strokeOpacity: 1,
            strokeWeight: 2,
            fillColor: '#1B5E3B',
            fillOpacity: 0.35,
          }),
        )
        path.forEach((point) => bounds.extend(point))
      }
    }
    if (hasPath) map.fitBounds(bounds, 48)
    else map.setCenter({ lat: -19.0, lng: -54.5 })

    return () => {
      polygonsRef.current.forEach((polygon) => polygon.setMap(null))
      polygonsRef.current = []
    }
  }, [mapReady, signature])

  if (!apiKey) {
    return (
      <Card className="flex h-full min-h-64 flex-col items-start gap-2 border-dashed">
        <div className="flex items-center gap-2 text-pf-green">
          <MapPinOff className="h-5 w-5" aria-hidden />
          <strong>Chave do Google Maps ausente</strong>
        </div>
        <p className="text-sm text-pf-muted">
          Defina <code>VITE_GOOGLE_MAPS_API_KEY</code> no arquivo <code>.env</code> para carregar o
          mapa satélite real. Nenhum mosaico fictício é exibido.
        </p>
      </Card>
    )
  }

  if (loadError) {
    return (
      <Card>
        <p className="text-sm text-red-800">{loadError}</p>
      </Card>
    )
  }

  return <div ref={containerRef} className={`w-full overflow-hidden rounded-[12px] ${className}`} />
}
