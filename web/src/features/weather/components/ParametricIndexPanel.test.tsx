import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { ParametricIndex } from '@/shared/api/types'

import { ParametricIndexPanel } from './ParametricIndexPanel'

const index: ParametricIndex = {
  farmId: 'farm-1',
  simulation: true,
  days: [
    { date: '2026-09-01', rainMm: 12.5, waterDeficitMm: 3.1, frostRisk: false },
    { date: '2026-09-02', rainMm: 0, waterDeficitMm: 5.0, frostRisk: true },
  ],
}

vi.mock('@/features/weather/queries', () => ({
  useParametricIndexQuery: () => ({
    data: index,
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

describe('ParametricIndexPanel', () => {
  it('renders rows from API', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <ParametricIndexPanel farmId="farm-1" />
      </QueryClientProvider>,
    )
    expect(screen.getByText(/12[,.]5/)).toBeInTheDocument()
    expect(screen.getByText(/3[,.]1/)).toBeInTheDocument()
    expect(screen.getAllByText(/0|5/).length).toBeGreaterThan(0)
  })
})
