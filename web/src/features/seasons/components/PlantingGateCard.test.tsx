import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { Season } from '@/features/seasons/types'
import type { PlantingGate } from '@/shared/api/types'

import { PlantingGateCard } from './PlantingGateCard'

const gate: PlantingGate = {
  farmId: 'farm-1',
  municipality: 'Sorriso',
  crop: 'SOY',
  date: '2026-10-01',
  decision: 'BLOCKED',
  reason: 'ZARC_OUT_OF_WINDOW',
  simulation: true,
}

const season: Season = {
  id: 's1',
  farmId: 'farm-1',
  name: 'Safra 26/27',
  crop: 'SOY',
  startDate: '2026-10-01',
  endDate: '2027-03-01',
}

const mutateSpy = vi.fn()

vi.mock('@/features/weather/queries', () => ({
  usePlantingGateQuery: () => ({
    data: gate,
    isLoading: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

describe('PlantingGateCard', () => {
  it('BLOCKED shows reason and infoOnly; no mutate', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <PlantingGateCard season={season} />
      </QueryClientProvider>,
    )
    expect(screen.getByText('Plantio bloqueado')).toBeInTheDocument()
    expect(screen.getByText(/Fora da janela de plantio do ZARC/)).toBeInTheDocument()
    expect(screen.getByText(/Somente informativo/)).toBeInTheDocument()
    expect(mutateSpy).not.toHaveBeenCalled()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
