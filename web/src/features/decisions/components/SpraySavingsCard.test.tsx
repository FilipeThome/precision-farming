import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { SpraySavings } from '@/shared/api/types'

import { SpraySavingsCard } from './SpraySavingsCard'

const savings: SpraySavings = {
  prescriptionId: 'rx-1',
  fieldId: 'field-1',
  mode: 'SPOT',
  fieldAreaHa: 40,
  treatedHa: 14,
  fullRateHa: 26,
  litersFullRate: 100,
  litersSpot: 35,
  litersAvoided: 65,
  litersPerHa: 2.5,
  unit: 'L',
  simulation: true,
}

vi.mock('@/features/agronomy/queries', () => ({
  useSpraySavingsQuery: () => ({
    data: savings,
    isLoading: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

describe('SpraySavingsCard', () => {
  it('shows API litersAvoided / litersPerHa and simulation notice', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <SpraySavingsCard prescriptionId="rx-1" />
      </QueryClientProvider>,
    )
    expect(screen.getByText(/65/)).toBeInTheDocument()
    expect(screen.getByText(/2[,.]5/)).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/simulad/i)
  })
})
