import { describe, expect, it } from 'vitest'

import {
  cropPhoto,
  inventoryPhoto,
  irrigationPhoto,
  machinePhoto,
  storagePhoto,
} from '@/shared/demo/media'

describe('demo media', () => {
  it('maps inventory SKUs to distinct photos', () => {
    expect(inventoryPhoto('GLYPHOSATE', 'PESTICIDE')).toBe('/demo/inventory/glyphosate.jpg')
    expect(inventoryPhoto('DIESEL_S10', 'FUEL')).toBe('/demo/inventory/diesel.jpg')
    expect(inventoryPhoto('COTTON_SEED', 'SEED')).toBe('/demo/inventory/cotton-seed.jpg')
    expect(inventoryPhoto('DRIVE_BELT', 'PART')).toBe('/demo/inventory/drive-belt.jpg')
    expect(inventoryPhoto('TWO_FOUR_D')).toBe('/demo/inventory/24d.jpg')
    expect(inventoryPhoto(undefined, 'FERTILIZER')).toBe('/demo/inventory/urea.jpg')
    expect(inventoryPhoto()).toBe('/demo/infra/inventory.jpg')
  })

  it('maps irrigation and storage types', () => {
    expect(irrigationPhoto('PIVOT')).toBe('/demo/infra/pivot.jpg')
    expect(irrigationPhoto('DRIP')).toBe('/demo/infra/drip.jpg')
    expect(irrigationPhoto('SPRINKLER')).toBe('/demo/infra/sprinkler.jpg')
    expect(irrigationPhoto('PUMP')).toBe('/demo/infra/pump.jpg')
    expect(irrigationPhoto('RESERVOIR')).toBe('/demo/infra/reservoir.jpg')
    expect(irrigationPhoto()).toBe('/demo/infra/irrigation.jpg')
    expect(storagePhoto('SILO')).toBe('/demo/infra/silo.jpg')
    expect(storagePhoto('WAREHOUSE')).toBe('/demo/infra/warehouse.jpg')
    expect(storagePhoto('WAREHOUSE_NE')).toBe('/demo/infra/warehouse.jpg')
  })

  it('leaves machinePhoto empty so farm fallback can run', () => {
    expect(machinePhoto(null)).toBeUndefined()
    expect(machinePhoto(undefined)).toBeUndefined()
    expect(machinePhoto('unknown-machine')).toBe('/demo/machines/machine-001.jpg')
  })

  it('maps crop names including Portuguese labels', () => {
    expect(cropPhoto('soja')).toBe('/demo/crops/soja.jpg')
    expect(cropPhoto('milho')).toBe('/demo/crops/milho.jpg')
    expect(cropPhoto('algodão')).toBe('/demo/crops/algodao.jpg')
    expect(cropPhoto()).toBe('/demo/crops/default.jpg')
  })
})
