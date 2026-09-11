import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { ControlTowerPage } from '@/features/dashboard/pages/ControlTowerPage'
import type { Farm, Operation } from '@/shared/api/types'

vi.mock('@/shared/maps/FieldMap', () => ({
  FieldMap: () => <div data-testid="field-map" />,
}))

const farm: Farm = { id: 'farm-1', name: 'Fazenda Alfa', location: 'MT', areaHa: 100, timezone: 'UTC' }

const completedOnTime: Operation = {
  id: 'op-1',
  fieldId: 'field-1',
  farmId: 'farm-1',
  type: 'SPRAYING',
  status: 'COMPLETED',
  plannedStart: '2026-09-01T08:00:00Z',
  plannedEnd: '2026-09-01T12:00:00Z',
  actualStart: '2026-09-01T08:00:00Z',
  actualEnd: '2026-09-01T11:00:00Z',
  machineId: 'm1',
  pauseReason: null,
  itemId: null,
  itemQuantity: null,
  areaHa: null,
}

function renderTower(seed: (client: QueryClient) => void) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, queryFn: () => Promise.resolve([]) } } })
  seed(client)
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <ControlTowerPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ControlTowerPage', () => {
  it('renders the north star from real operations and the mocked map', async () => {
    renderTower((client) => {
      client.setQueryData(['farms'], [farm])
      client.setQueryData(['operations', 'all'], [completedOnTime, { ...completedOnTime, id: 'op-2', actualEnd: '2026-09-01T13:00:00Z' }])
      client.setQueryData(['machines', 'all'], [])
      client.setQueryData(['alerts', 'all'], [])
      client.setQueryData(['fields', 'all'], [])
      client.setQueryData(['weather', 'windows', 'all'], [])
      client.setQueryData(['agronomy', 'prescriptions', 'all'], [])
      client.setQueryData(['agronomy', 'recommendations', 'all'], [])
      client.setQueryData(['irrigation', 'recommendations', 'all'], [])
      client.setQueryData(['ai', 'insights', 'all'], [])
    })
    expect(await screen.findByText('Control Tower')).toBeInTheDocument()
    expect(screen.getByText('50%')).toBeInTheDocument()
    expect(screen.getByText('1 de 2 operações concluídas dentro da janela')).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).toBeNull()
    expect(screen.getByText('Nenhum talhão encontrado')).toBeInTheDocument()
  })

  it('shows the empty state when there are no farms', async () => {
    renderTower((client) => {
      client.setQueryData(['farms'], [])
    })
    expect(await screen.findByText('Nenhuma fazenda para monitorar')).toBeInTheDocument()
  })
})
