import { describe, expect, it } from 'vitest'

import { centroidOfGeometry, geometryForFieldSave, squarePolygon } from '@/shared/maps/geometry'

describe('squarePolygon', () => {
  it('builds a closed GeoJSON polygon around the point', () => {
    const raw = squarePolygon(-22.9, -49.9, 100)
    const parsed = JSON.parse(raw) as { type: string; coordinates: number[][][] }
    expect(parsed.type).toBe('Polygon')
    const ring = parsed.coordinates[0]
    expect(ring[0]).toEqual(ring[ring.length - 1])
    const c = centroidOfGeometry(raw)
    expect(c?.lat).toBeCloseTo(-22.9, 3)
    expect(c?.lon).toBeCloseTo(-49.9, 3)
  })
})

describe('geometryForFieldSave', () => {
  it('keeps the existing polygon when lat lon and area did not change', () => {
    const existing = '{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,0]]]]}'
    expect(
      geometryForFieldSave({
        existingGeometry: existing,
        originalLat: '-19.39',
        originalLon: '-54.57',
        originalAreaHa: '120.5',
        lat: '-19.39',
        lon: '-54.57',
        areaHa: '120.5',
      }),
    ).toBe(existing)
  })

  it('builds a square when the operator changes location', () => {
    const existing = '{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,0]]]]}'
    const next = geometryForFieldSave({
      existingGeometry: existing,
      originalLat: '-19.39',
      originalLon: '-54.57',
      originalAreaHa: '120.5',
      lat: '-22.9',
      lon: '-49.9',
      areaHa: '120.5',
    })
    expect(next).not.toBe(existing)
    expect(JSON.parse(next).type).toBe('Polygon')
  })
})
