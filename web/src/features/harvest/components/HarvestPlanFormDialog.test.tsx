import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Field } from '@/shared/api/types'

const useFieldsQuery = vi.fn()
const mutateAsync = vi.fn()

vi.mock('@/features/fields/queries', () => ({
  useFieldsQuery: (...args: unknown[]) => useFieldsQuery(...args),
}))

vi.mock('@/features/harvest/queries', () => ({
  useHarvestPlanCommands: () => ({
    isPending: false,
    error: null,
    mutateAsync: (...args: unknown[]) => mutateAsync(...args),
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

import { HarvestPlanFormDialog } from './HarvestPlanFormDialog'

function fieldRow(id: string, farmId = 'farm-1'): Field {
  return { id, farmId, name: `Talhão ${id}`, areaHa: 10, crop: 'SOY', variety: null, geometry: '' }
}

describe('HarvestPlanFormDialog', () => {
  beforeEach(() => {
    mutateAsync.mockReset()
    mutateAsync.mockResolvedValue({ id: 'plan-1' })
    useFieldsQuery.mockReset()
  })

  it('disables Save until the selected field is in the scoped list', async () => {
    useFieldsQuery.mockReturnValue({ isSuccess: false, data: undefined })
    const { rerender } = render(<HarvestPlanFormDialog open onClose={() => undefined} />)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()

    useFieldsQuery.mockReturnValue({ isSuccess: true, data: [fieldRow('field-1')] })
    rerender(<HarvestPlanFormDialog open onClose={() => undefined} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled())
  })

  it('sends null planned dates when both are empty', async () => {
    useFieldsQuery.mockReturnValue({ isSuccess: true, data: [fieldRow('field-1')] })
    render(<HarvestPlanFormDialog open onClose={() => undefined} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled())
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({ plannedStart: null, plannedEnd: null, fieldId: 'field-1' }),
    )
  })

  it('sends an ISO pair when both dates are filled', async () => {
    useFieldsQuery.mockReturnValue({ isSuccess: true, data: [fieldRow('field-1')] })
    render(<HarvestPlanFormDialog open onClose={() => undefined} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled())
    fireEvent.change(screen.getByLabelText('Início'), { target: { value: '2026-09-01' } })
    fireEvent.change(screen.getByLabelText('Fim'), { target: { value: '2026-09-10' } })
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        plannedStart: '2026-09-01T00:00:00.000Z',
        plannedEnd: '2026-09-10T23:59:59.000Z',
      }),
    )
  })

  it('does not mutate when only one date is filled', async () => {
    useFieldsQuery.mockReturnValue({ isSuccess: true, data: [fieldRow('field-1')] })
    render(<HarvestPlanFormDialog open onClose={() => undefined} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled())
    fireEvent.change(screen.getByLabelText('Início'), { target: { value: '2026-09-01' } })
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(mutateAsync).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Informe o início e o fim, ou deixe ambos em branco.')
  })
})
