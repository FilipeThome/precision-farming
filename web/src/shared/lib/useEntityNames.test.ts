import { describe, expect, it } from 'vitest'

import type { Farm, Field, Machine } from '@/shared/api/types'
import { buildEntityNameMap, shortEntityId } from '@/shared/lib/useEntityNames'

const farm: Farm = { id: 'farm-1', name: 'Fazenda Alfa', location: 'MT', areaHa: 100, timezone: 'UTC' }
const field: Field = { id: 'field-1', farmId: 'farm-1', name: 'Talhão Norte', areaHa: 10, crop: 'SOY', variety: null, geometry: '' }
const machine: Machine = { id: 'm-1', farmId: 'farm-1', name: 'Trator 01', type: 'TRACTOR', manufacturer: 'X', model: 'Y', status: 'IDLE' }

describe('buildEntityNameMap', () => {
  it('merges names from farm, field and machine caches', () => {
    const map = buildEntityNameMap({
      farms: [[['farms'], [farm]]],
      fields: [[['fields', 'all'], [field]], [['fields', 'farm-1'], undefined]],
      machines: [[['machines', 'all'], [machine]]],
    })
    expect(map.get('farm-1')).toBe('Fazenda Alfa')
    expect(map.get('field-1')).toBe('Talhão Norte')
    expect(map.get('m-1')).toBe('Trator 01')
    expect(map.get('unknown')).toBeUndefined()
  })

  it('shortens uuids and keeps other ids intact', () => {
    expect(shortEntityId('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).toBe('aaaaaaaa')
    expect(shortEntityId('m-1')).toBe('m-1')
  })
})
