import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { MoaRotation, Prescription } from '@/shared/api/types'

import { MoaWarningsPanel } from './MoaWarningsPanel'

const prescriptions: Prescription[] = [
  {
    id: 'rx-1',
    farmId: 'farm-1',
    fieldId: 'field-1',
    product: 'A',
    plannedDose: 1,
    unit: 'L/ha',
    status: 'APPROVED',
    createdAt: '2026-09-10T08:00:00Z',
    approvedAt: '2026-09-10T09:00:00Z',
  },
  {
    id: 'rx-2',
    farmId: 'farm-1',
    fieldId: 'field-2',
    product: 'B',
    plannedDose: 1,
    unit: 'L/ha',
    status: 'DRAFT',
    createdAt: '2026-09-10T08:00:00Z',
    approvedAt: null,
  },
]

const mockState: { rotations: Array<MoaRotation | undefined> } = { rotations: [] }

vi.mock('@/features/agronomy/queries', () => ({
  usePrescriptionsQuery: () => ({
    data: prescriptions,
    isLoading: false,
    isSuccess: true,
    error: null,
    refetch: vi.fn(),
  }),
  useMoaRotationQueries: () =>
    mockState.rotations.map((data) => ({
      data,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    })),
}))

describe('MoaWarningsPanel', () => {
  it('shows empty state when no warnings', () => {
    mockState.rotations = [
      { fieldId: 'field-1', moaGroup: 'G', warning: false, prescriptionIds: ['rx-1'], simulation: true },
      { fieldId: 'field-2', moaGroup: 'A', warning: false, prescriptionIds: ['rx-2'], simulation: true },
    ]
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <MoaWarningsPanel farmId="farm-1" />
      </QueryClientProvider>,
    )
    expect(screen.getByText('Sem alertas de MoA')).toBeInTheDocument()
  })

  it('lists only rows with warning=true', () => {
    mockState.rotations = [
      { fieldId: 'field-1', moaGroup: 'G', warning: true, prescriptionIds: ['rx-1'], simulation: true },
      { fieldId: 'field-2', moaGroup: 'A', warning: false, prescriptionIds: ['rx-2'], simulation: true },
    ]
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <MoaWarningsPanel farmId="farm-1" />
      </QueryClientProvider>,
    )
    expect(screen.getByText('G')).toBeInTheDocument()
    expect(screen.queryByText('A')).not.toBeInTheDocument()
    expect(screen.getByText(/Mesmo MoA/)).toBeInTheDocument()
  })
})
