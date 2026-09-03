import { describe, expect, it } from 'vitest'

import { geometryToPaths, shortId } from '@/shared/maps/geometry'
import { fieldPolygons, fieldSetKey, fitBoundsOf } from '@/shared/maps/fieldMapModel'
import type { Field, MapLayer } from '@/shared/api/types'

const soy: Field = {
  id: 'field-a',
  farmId: 'farm-a',
  name: 'Talhão 01',
  areaHa: 10,
  crop: 'SOY',
  variety: null,
  geometry: JSON.stringify({
    type: 'Polygon',
    coordinates: [
      [
        [-54.57, -19.39],
        [-54.53, -19.39],
        [-54.53, -19.35],
        [-54.57, -19.35],
        [-54.57, -19.39],
      ],
    ],
  }),
}

const corn: Field = {
  ...soy,
  id: 'field-b',
  name: 'Talhão 02',
  crop: 'CORN',
  geometry: JSON.stringify({
    type: 'Polygon',
    coordinates: [
      [
        [-50.92, -17.79],
        [-50.88, -17.79],
        [-50.88, -17.75],
        [-50.92, -17.75],
        [-50.92, -17.79],
      ],
    ],
  }),
}

describe('geometryToPaths', () => {
  it('parses polygon rings as lat/lng', () => {
    const rings = geometryToPaths(soy.geometry)
    expect(rings[0][0]).toEqual({ lat: -19.39, lng: -54.57 })
  })
})

describe('fieldPolygons', () => {
  it('fits bounds to the active farm fields', () => {
    const a = fieldPolygons([soy], [])
    const b = fieldPolygons([corn], [])
    expect(fitBoundsOf(a)?.[0][1]).toBeCloseTo(-54.57)
    expect(fitBoundsOf(b)?.[0][1]).toBeCloseTo(-50.92)
  })

  it('changes fill when NDVI layer is toggled for the field', () => {
    const layers: MapLayer[] = [
      {
        id: 'l1',
        farmId: 'farm-a',
        fieldId: 'field-a',
        name: 'NDVI',
        kind: 'NDVI',
        source: 'DEMO',
        tileUrl: null,
        acquiredAt: null,
        status: 'READY',
      },
    ]
    const off = fieldPolygons([soy], [], layers)
    const on = fieldPolygons([soy], ['NDVI'], layers)
    expect(on[0].fillColor).not.toBe(off[0].fillColor)
    expect(on[0].fillOpacity).toBeGreaterThan(off[0].fillOpacity)
  })
})

describe('shortId', () => {
  it('keeps unknown ids distinct', () => {
    expect(shortId('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).toBe('aaaaaaaa')
    expect(shortId('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')).toBe('bbbbbbbb')
  })
})

describe('fieldSetKey', () => {
  it('is stable for the same field ids regardless of order', () => {
    expect(fieldSetKey([corn, soy])).toBe(fieldSetKey([soy, corn]))
    expect(fieldSetKey([soy])).not.toBe(fieldSetKey([soy, corn]))
  })
})
