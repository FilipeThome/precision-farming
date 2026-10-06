import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Field } from '@/features/fields/types'

const createMutate = vi.fn()
const patchMutate = vi.fn()

vi.mock('@/features/fields/queries', () => ({
  useFieldCommands: () => ({
    create: { isPending: false, error: null, mutateAsync: (...args: unknown[]) => createMutate(...args) },
    patch: { isPending: false, error: null, mutateAsync: (...args: unknown[]) => patchMutate(...args) },
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
      options: [
        { value: 'farm-1', label: 'Alfa' },
        { value: 'farm-other', label: 'Beta' },
      ],
    },
  }),
  useCodeOptions: (codes: string[]) => codes.map((value) => ({ value, label: value })),
}))

import { FieldFormDialog } from './FieldFormDialog'

const field: Field = {
  id: 'field-1',
  farmId: 'farm-original',
  name: 'Talhão 01',
  areaHa: 12,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

describe('FieldFormDialog', () => {
  beforeEach(() => {
    createMutate.mockReset()
    patchMutate.mockReset()
    patchMutate.mockResolvedValue(field)
  })

  it('shows the farm select on create', () => {
    render(<FieldFormDialog open field={null} onClose={() => undefined} />)
    expect(screen.getByLabelText('Fazenda')).toBeInTheDocument()
  })

  it('omits the farm select on edit', () => {
    render(<FieldFormDialog open field={field} onClose={() => undefined} />)
    expect(screen.queryByLabelText('Fazenda')).not.toBeInTheDocument()
  })

  it('patches with the original farmId', () => {
    render(<FieldFormDialog open field={field} onClose={() => undefined} />)
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(patchMutate).toHaveBeenCalledWith({
      id: 'field-1',
      body: expect.objectContaining({ farmId: 'farm-original' }),
    })
  })
})
