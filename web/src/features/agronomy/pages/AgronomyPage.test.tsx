import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { approvePrescription, fetchPrescriptions, fetchScouting } from '@/features/agronomy/api'
import { AgronomyPage } from '@/features/agronomy/pages/AgronomyPage'
import { useAuthStore } from '@/shared/auth/store'
import type { Prescription } from '@/features/agronomy/types'
import { useUiStore } from '@/shared/ui/uiStore'

vi.mock('@/features/agronomy/api', () => ({
  fetchScouting: vi.fn(),
  fetchSoilSamples: vi.fn(),
  fetchRecommendations: vi.fn(),
  fetchPrescriptions: vi.fn(),
  fetchPrescription: vi.fn(),
  approvePrescription: vi.fn(),
  rejectPrescription: vi.fn(),
  fetchSpraySavings: vi.fn(),
  fetchMoaRotation: vi.fn(),
}))

const fetchScoutingMock = vi.mocked(fetchScouting)
const fetchPrescriptionsMock = vi.mocked(fetchPrescriptions)
const approvePrescriptionMock = vi.mocked(approvePrescription)

const prescription: Prescription = {
  id: 'rx-1',
  farmId: 'farm-1',
  fieldId: 'field-1',
  product: 'Glifosato',
  plannedDose: 2,
  unit: 'L/ha',
  status: 'DRAFT',
  createdAt: '2026-09-10T12:00:00Z',
  approvedAt: null,
}

function renderAgronomy() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/agronomy']}>
        <AgronomyPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AgronomyPage', () => {
  beforeEach(() => {
    fetchScoutingMock.mockReset()
    fetchPrescriptionsMock.mockReset()
    approvePrescriptionMock.mockReset()
    fetchScoutingMock.mockResolvedValue([])
    fetchPrescriptionsMock.mockResolvedValue([prescription])
    approvePrescriptionMock.mockResolvedValue({ ...prescription, status: 'APPROVED', approvedAt: '2026-09-10T13:00:00Z' })
    useUiStore.setState({ farmId: null })
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      role: 'FARM_MANAGER',
      userId: 'user-1',
      name: 'Ana',
      email: 'ana@example.com',
    })
  })

  afterEach(() => {
    useAuthStore.getState().clearSession()
    useUiStore.setState({ farmId: null })
  })

  it('approves a draft prescription', async () => {
    const user = userEvent.setup()
    renderAgronomy()
    await user.click(await screen.findByRole('button', { name: 'Receitas' }))
    await user.click(await screen.findByRole('button', { name: 'Aprovar' }))
    await waitFor(() => expect(approvePrescriptionMock).toHaveBeenCalledWith('rx-1'))
  })
})