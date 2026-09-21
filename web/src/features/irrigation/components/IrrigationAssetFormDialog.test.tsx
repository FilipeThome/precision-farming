import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Field, IrrigationAsset } from '@/shared/api/types'

const useFieldsQuery = vi.fn()
const createMutate = vi.fn()
const patchMutate = vi.fn()

vi.mock('@/features/fields/queries', () => ({
  useFieldsQuery: (...args: unknown[]) => useFieldsQuery(...args),
}))

vi.mock('@/features/irrigation/queries', () => ({
  useIrrigationAssetCommands: () => ({
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
      options: [{ value: 'farm-1', label: 'Alfa' }],
    },
  }),
  useCodeOptions: (codes: string[]) => codes.map((value) => ({ value, label: value })),
}))

import { IrrigationAssetFormDialog } from './IrrigationAssetFormDialog'

function fieldRow(id: string, farmId = 'farm-1'): Field {
  return { id, farmId, name: `Talhão ${id}`, areaHa: 10, crop: 'SOY', variety: null, geometry: '' }
}

const staleAsset: IrrigationAsset = {
  id: 'asset-1',
  farmId: 'farm-1',
  fieldId: 'stale-field',
  name: 'Pivô Norte',
  type: 'PIVOT',
  status: 'IDLE',
}

describe('IrrigationAssetFormDialog', () => {
  beforeEach(() => {
    createMutate.mockReset()
    patchMutate.mockReset()
    createMutate.mockResolvedValue({ id: 'asset-1' })
    useFieldsQuery.mockReset()
  })

  it('can save with an empty fieldId', () => {
    useFieldsQuery.mockReturnValue({ isSuccess: false, data: undefined })
    render(<IrrigationAssetFormDialog open asset={null} onClose={() => undefined} />)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled()
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(createMutate).toHaveBeenCalledWith(expect.objectContaining({ fieldId: null, farmId: 'farm-1' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('disables Save and does not mutate when fieldId is stale', () => {
    useFieldsQuery.mockReturnValue({ isSuccess: true, data: [fieldRow('field-1')] })
    render(<IrrigationAssetFormDialog open asset={staleAsset} onClose={() => undefined} />)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(createMutate).not.toHaveBeenCalled()
    expect(patchMutate).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('O talhão selecionado não pertence à fazenda.')
  })
})
