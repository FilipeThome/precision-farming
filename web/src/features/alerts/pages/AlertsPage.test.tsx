import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ackAlert, fetchAlerts } from '@/features/alerts/api'
import { AlertsPage } from '@/features/alerts/pages/AlertsPage'
import type { Alert } from '@/features/alerts/types'

vi.mock('@/features/alerts/api', () => ({
  fetchAlerts: vi.fn(),
  ackAlert: vi.fn(),
}))

const fetchAlertsMock = vi.mocked(fetchAlerts)
const ackAlertMock = vi.mocked(ackAlert)

const alert: Alert = {
  id: 'alert-1',
  farmId: 'farm-1',
  severity: 'WARNING',
  type: 'PEST',
  title: 'Ferrugem',
  message: 'Foco no talhão',
  entityType: null,
  entityId: null,
  status: 'OPEN',
  createdAt: '2026-09-10T12:00:00Z',
}

function renderAlerts() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/alerts']}>
        <AlertsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AlertsPage', () => {
  beforeEach(() => {
    fetchAlertsMock.mockReset()
    ackAlertMock.mockReset()
    fetchAlertsMock.mockResolvedValue([alert])
    ackAlertMock.mockResolvedValue({ ...alert, status: 'ACKED' })
  })

  it('acknowledges an open alert', async () => {
    const user = userEvent.setup()
    renderAlerts()
    await user.click(await screen.findByRole('button', { name: 'Reconhecer' }))
    await waitFor(() => expect(ackAlertMock.mock.calls[0]?.[0]).toBe('alert-1'))
  })
})
