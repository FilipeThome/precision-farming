import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { CreditDossier } from '@/features/compliance/types'

import { CreditDossierPanel } from './CreditDossierPanel'

const dossier: CreditDossier = {
  farmId: 'farm-1',
  carCode: 'CAR-001',
  carStatus: 'ACTIVE',
  embargoed: false,
  deforestationCutoffDate: '2008-07-22',
  deforestationClear: true,
  zarcCompliant: true,
  remoteSensingNote: 'OK',
  simulation: true,
}

vi.mock('@/features/compliance/queries', () => ({
  useCreditDossierQuery: () => ({
    data: dossier,
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

describe('CreditDossierPanel', () => {
  it('success shows simulation notice', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <CreditDossierPanel farmId="farm-1" />
      </QueryClientProvider>,
    )
    expect(screen.getByRole('status')).toHaveTextContent(/Simulação|simulad/i)
  })
})
