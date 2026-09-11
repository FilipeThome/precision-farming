import { useEffect, useRef, useState } from 'react'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import type { Field, MapLayer } from '@/shared/api/types'
import { fieldPolygons, fieldSetKey, fitBoundsOf } from '@/shared/maps/fieldMapModel'
import type { FieldState } from '@/shared/maps/fieldStateColors'
import { useI18n } from '@/shared/i18n/useI18n'

type FieldMapProps = {
  fields: Field[]
  className?: string
  activeLayerKinds?: string[]
  layers?: MapLayer[]
  /** When given, polygons are colored by operation state instead of crop/layer. */
  fieldStates?: Record<string, FieldState>
  /** Minimum height class of the map canvas. */
  minHeightClass?: string
  onFieldClick?: (fieldId: string) => void
}

const ESRI_IMAGERY = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
const ESRI_ATTRIBUTION =
  'Tiles © Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community'

function tooltipElement(text: string): HTMLElement {
  const el = document.createElement('span')
  el.textContent = text
  return el
}

export function FieldMap({
  fields,
  className = 'h-[520px]',
  activeLayerKinds = [],
  layers = [],
  fieldStates,
  minHeightClass = 'min-h-[560px]',
  onFieldClick,
}: FieldMapProps) {
  const { t } = useI18n()
  const elRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<L.Map | null>(null)
  const groupRef = useRef<L.FeatureGroup | null>(null)
  const fittedKeyRef = useRef<string | null>(null)
  const onFieldClickRef = useRef(onFieldClick)
  onFieldClickRef.current = onFieldClick
  const [loadError, setLoadError] = useState(false)
  const setKey = fieldSetKey(fields)

  useEffect(() => {
    const el = elRef.current
    if (!el || mapRef.current) return

    const map = L.map(el, { scrollWheelZoom: true, attributionControl: true })
    const tiles = L.tileLayer(ESRI_IMAGERY, {
      attribution: ESRI_ATTRIBUTION,
      maxZoom: 19,
    })
    tiles.on('tileerror', () => setLoadError(true))
    tiles.addTo(map)
    const group = L.featureGroup().addTo(map)
    mapRef.current = map
    groupRef.current = group
    const resize = window.setTimeout(() => map.invalidateSize(), 50)
    return () => {
      window.clearTimeout(resize)
      map.remove()
      mapRef.current = null
      groupRef.current = null
      fittedKeyRef.current = null
    }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    const group = groupRef.current
    if (!map || !group) return

    group.clearLayers()
    const polygons = fieldPolygons(fields, activeLayerKinds, layers, fieldStates)
    for (const poly of polygons) {
      for (const ring of poly.rings) {
        const latlngs = ring.map((p) => [p.lat, p.lng] as [number, number])
        L.polygon(latlngs, {
          color: poly.color,
          fillColor: poly.fillColor,
          fillOpacity: poly.fillOpacity,
          weight: poly.weight,
          dashArray: poly.dashArray,
        })
          .bindTooltip(tooltipElement(poly.name))
          .on('click', () => onFieldClickRef.current?.(poly.id))
          .addTo(group)
      }
    }

    if (fittedKeyRef.current !== setKey) {
      fittedKeyRef.current = setKey
      const bounds = fitBoundsOf(polygons)
      if (bounds) {
        map.fitBounds(bounds, { padding: [28, 28], maxZoom: 15 })
      } else {
        map.setView([-16.5, -54.5], 5)
      }
    }
    map.invalidateSize()
  }, [fields, activeLayerKinds, layers, fieldStates, setKey])

  return (
    <div className={`w-full overflow-hidden rounded-[14px] border border-ag-n-200 ${className}`}>
      {loadError ? (
        <p className="mb-2 text-sm text-ag-crit" role="alert">
          {t('map.loadError')}
        </p>
      ) : null}
      <div ref={elRef} className={`h-full w-full ${minHeightClass}`} role="application" aria-label={t('map.title')} />
    </div>
  )
}
