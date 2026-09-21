import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Machine } from '@/shared/api/types'

const useMachinesQuery = vi.fn()
const mutateAsync = vi.fn()

vi.mock('@/features/machines/queries', () => ({
  useMachinesQuery: (...args: unknown[]) => useMachinesQuery(...args),
}))

vi.mock('@/features/maintenance/queries', () => ({
  useWorkOrderCommands: () => ({
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

import { WorkOrderFormDialog } from './WorkOrderFormDialog'

function machineRow(id: string, farmId = 'farm-1'): Machine {
  return {
    id,
    farmId,
    name: `Máquina ${id}`,
    type: 'TRACTOR',
    manufacturer: 'JD',
    model: '8R',
    status: 'IDLE',
  }
}

describe('WorkOrderFormDialog', () => {
  beforeEach(() => {
    mutateAsync.mockReset()
    useMachinesQuery.mockReset()
  })

  it('disables Save until the selected machine is in the scoped list', async () => {
    useMachinesQuery.mockReturnValue({ isSuccess: false, data: undefined })
    const { rerender } = render(<WorkOrderFormDialog open onClose={() => undefined} />)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()

    useMachinesQuery.mockReturnValue({ isSuccess: true, data: [machineRow('machine-1')] })
    rerender(<WorkOrderFormDialog open onClose={() => undefined} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled())
  })
})
