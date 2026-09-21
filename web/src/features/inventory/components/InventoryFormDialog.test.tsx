import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { InventoryItem } from '@/shared/api/types'
import { INVENTORY_UNITS } from '@/features/inventory/units'

vi.mock('@/features/inventory/queries', () => ({
  useInventoryCommands: () => ({
    create: { isPending: false, error: null, mutateAsync: vi.fn() },
    patch: { isPending: false, error: null, mutateAsync: vi.fn() },
  }),
}))

vi.mock('@/shared/ui/useFarmScope', () => ({
  useFarmScope: () => ({
    farmId: 'farm-1',
    defaultFarmId: 'farm-1',
    farmField: {
      name: 'farmId',
      label: 'Fazenda',
      type: 'select',
      required: true,
      options: [{ value: 'farm-1', label: 'Alfa' }],
    },
  }),
  useCodeOptions: (codes: string[]) => codes.map((value) => ({ value, label: value })),
}))

import { InventoryFormDialog } from './InventoryFormDialog'

function optionValues(select: HTMLElement) {
  return [...select.querySelectorAll('option')].map((opt) => (opt as HTMLOptionElement).value)
}

describe('InventoryFormDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses a select of allowed units and defaults to KG', () => {
    render(<InventoryFormDialog open item={null} onClose={() => undefined} />)
    const select = screen.getByLabelText('Unidade')
    expect(select.tagName).toBe('SELECT')
    expect(optionValues(select)).toEqual([...INVENTORY_UNITS])
    expect(select).toHaveValue('KG')
  })

  it('appends the current unit when editing an item outside the allowed set', () => {
    const item: InventoryItem = {
      id: 'item-1',
      farmId: 'farm-1',
      name: 'Sementes',
      category: 'SEED',
      unit: 'BAG',
      quantity: 4,
      reserved: 0,
    }
    render(<InventoryFormDialog open item={item} onClose={() => undefined} />)
    expect(optionValues(screen.getByLabelText('Unidade'))).toEqual([...INVENTORY_UNITS, 'BAG'])
    expect(screen.getByLabelText('Unidade')).toHaveValue('BAG')
  })
})
