import { useEffect, useRef, useState } from 'react'
import { MapPinOff } from 'lucide-react'

import type { Field } from '@/shared/api/types'
import { Card } from '@/shared/ui/Card'

import { geometryToPaths, getGoogleMapsApiKey, loadGoogleMaps } from './loadGoogleMaps'

type FieldMapProps = {
  fields: Field[]
  className?: string
}

export function FieldMap({ fields, className = 'h-[520px]' }: FieldMapProps) {
  const apiKey = getGoogleMapsApiKey()
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    if (!apiKey || !containerRef.current) return
    const node = containerRef.current
    let map: google.maps.Map | undefined
    let cancelled = false

    loadGoogleMaps(apiKey)
      .then((maps) => {
        if (cancelled || !node) return
        map = new maps.Map(node, {
          mapTypeId: maps.MapTypeId.SATELLITE,
          tilt: 0,
          streetViewControl: false,
          fullscreenControl: true,
        })
        const bounds = new maps.LatLngBounds()
        let hasPath = false
        for (const field of fields) {
          for (const path of geometryToPaths(field.geometry)) {
            if (path.length === 0) continue
            hasPath = true
            new maps.Polygon({
              map,
              paths: path,
              strokeColor: '#0D9488',
              strokeOpacity: 1,
              strokeWeight: 2,
              fillColor: '#1B5E3B',
              fillOpacity: 0.35,
            })
            path.forEach((point) => bounds.extend(point))
          }
        }
        if (hasPath) map.fitBounds(bounds, 48)
        else map.setCenter({ lat: -19.0, lng: -54.5 })
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : 'Falha ao carregar o mapa')
      })

    return () => {
      cancelled = true
    }
  }, [apiKey, fields])

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
