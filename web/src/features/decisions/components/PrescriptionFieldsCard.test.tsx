import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Prescription } from '@/shared/api/types'
import { useAuthStore } from '@/shared/auth/store'

import { PrescriptionFieldsCard } from './PrescriptionFieldsCard'

vi.mock('@/features/agronomy/queries', async () => {
  const actual = await vi.importActual<typeof import('@/features/agronomy/queries')>(
    '@/features/agronomy/queries',
  )
  return {
    ...actual,
    usePrescriptionQuery: () => ({
      data: undefined,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    }),
    useApprovePrescription: () => ({
      mutate: vi.fn(),
      isPending: false,
      error: null,
      isSuccess: false,
    }),
  }
})

const fixture: Prescription = {
  id: 'rx-1',
  farmId: 'farm-1',
  fieldId: 'field-1',
  product: 'GLIFOSATO',
  mode: 'SPOT',
  treatedFraction: 0.35,
  plannedDose: 2.5,
  unit: 'L/ha',
  activeIngredient: 'glyphosate',
  moaGroup: 'G',
  receituarioNumber: 'REC-001',
  responsibleTechCpf: '000.000.000-00',
  phiDays: 7,
  reentryHours: 24,
  fieldAreaHa: 40,
  status: 'DRAFT',
  createdAt: '2026-09-10T08:00:00Z',
  approvedAt: null,
}

function renderCard(rx: Prescription, role: string | null = 'FARM_MANAGER') {
  useAuthStore.setState({ role, name: 'RT', accessToken: 't', refreshToken: 'r' })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <PrescriptionFieldsCard prescription={rx} />
    </QueryClientProvider>,
  )
}

describe('PrescriptionFieldsCard', () => {
  beforeEach(() => {
    useAuthStore.setState({ role: null, name: null, accessToken: null, refreshToken: null })
  })

  it('renders fixture prescription fields', () => {
    renderCard(fixture)
    expect(screen.getByText('Localizado')).toBeInTheDocument()
    expect(screen.getByText('Glifosato')).toBeInTheDocument()
    expect(screen.getByText('G')).toBeInTheDocument()
    expect(screen.getByText('REC-001')).toBeInTheDocument()
    expect(screen.getByText('000.000.000-00')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('24')).toBeInTheDocument()
  })

  it('shows approve only when DRAFT and canManage', () => {
    const { rerender } = renderCard(fixture, 'FARM_MANAGER')
    expect(screen.getByRole('button', { name: 'Aprovar' })).toBeInTheDocument()

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    useAuthStore.setState({ role: 'OPERATOR', name: 'Op', accessToken: 't', refreshToken: 'r' })
    rerender(
      <QueryClientProvider client={client}>
        <PrescriptionFieldsCard prescription={fixture} />
      </QueryClientProvider>,
    )
    expect(screen.queryByRole('button', { name: 'Aprovar' })).not.toBeInTheDocument()

    useAuthStore.setState({ role: 'FARM_MANAGER', name: 'RT', accessToken: 't', refreshToken: 'r' })
    rerender(
      <QueryClientProvider client={client}>
        <PrescriptionFieldsCard prescription={{ ...fixture, status: 'APPROVED' }} />
      </QueryClientProvider>,
    )
    expect(screen.queryByRole('button', { name: 'Aprovar' })).not.toBeInTheDocument()
  })
})
