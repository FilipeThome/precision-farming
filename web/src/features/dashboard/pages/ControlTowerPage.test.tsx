import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ControlTowerPage } from '@/features/dashboard/pages/ControlTowerPage'
import { fetchFields } from '@/features/fields/api'
import { fetchOperations } from '@/features/operations/api'
import { ApiError } from '@/shared/api/client'
import type { Farm, Field, Operation } from '@/shared/api/types'

vi.mock('@/shared/maps/FieldMap', () => ({
  FieldMap: () => <div data-testid="field-map" />,
}))

vi.mock('@/features/fields/api', () => ({
  fetchFields: vi.fn(),
}))

vi.mock('@/features/operations/api', () => ({
  fetchOperations: vi.fn(),
  fetchMachineWorkSummary: vi.fn(),
  startOperation: vi.fn(),
  pauseOperation: vi.fn(),
  completeOperation: vi.fn(),
}))

const fetchFieldsMock = vi.mocked(fetchFields)
const fetchOperationsMock = vi.mocked(fetchOperations)

const farm: Farm = { id: 'farm-1', name: 'Fazenda Alfa', location: 'MT', areaHa: 100, timezone: 'UTC' }

const field: Field = {
  id: 'field-1',
  farmId: 'farm-1',
  name: 'Talhão 01',
  areaHa: 10,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

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

function seedTowerExtras(client: QueryClient) {
  client.setQueryData(['machines', 'all'], [])
  client.setQueryData(['alerts', 'all'], [])
  client.setQueryData(['weather', 'windows', 'all'], [])
  client.setQueryData(['agronomy', 'prescriptions', 'all'], [])
  client.setQueryData(['agronomy', 'recommendations', 'all'], [])
  client.setQueryData(['irrigation', 'recommendations', 'all'], [])
  client.setQueryData(['ai', 'insights', 'all'], [])
}

function renderTower(seed: (client: QueryClient) => void) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
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
  beforeEach(() => {
    fetchFieldsMock.mockReset()
    fetchOperationsMock.mockReset()
    fetchFieldsMock.mockResolvedValue([])
    fetchOperationsMock.mockResolvedValue([])
  })

  it('renders the north star from real operations and the mocked map', async () => {
    renderTower((client) => {
      client.setQueryData(['farms'], [farm])
      client.setQueryData(['operations', 'all'], [completedOnTime, { ...completedOnTime, id: 'op-2', actualEnd: '2026-09-01T13:00:00Z' }])
      client.setQueryData(['fields', 'all'], [])
      seedTowerExtras(client)
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

  it('stays busy while operations are still loading', async () => {
    fetchOperationsMock.mockImplementation(() => new Promise(() => {}))
    renderTower((client) => {
      client.setQueryData(['farms'], [farm])
      client.setQueryData(['fields', 'all'], [field])
      seedTowerExtras(client)
    })
    expect(await screen.findByRole('status', { busy: true })).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })

  it('fails the map when operations error instead of painting every field as none', async () => {
    fetchOperationsMock.mockRejectedValue(new ApiError('ops down', 500, 'cid-ops'))
    renderTower((client) => {
      client.setQueryData(['farms'], [farm])
      client.setQueryData(['fields', 'all'], [field])
      seedTowerExtras(client)
    })
    expect(await screen.findByText('cid-ops')).toBeInTheDocument()
    expect(screen.getAllByText('ops down').length).toBeGreaterThan(0)
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })
})
