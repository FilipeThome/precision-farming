import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { MapPage } from '@/features/map/pages/MapPage'
import { fetchFields } from '@/features/fields/api'
import { fetchOperations } from '@/features/operations/api'
import { ApiError } from '@/shared/api/client'
import type { Field, MapLayer, Operation } from '@/shared/api/types'

vi.mock('@/shared/maps/FieldMap', () => ({
  FieldMap: ({ fieldStates }: { fieldStates?: Record<string, string> }) => (
    <div data-testid="field-map" data-states={JSON.stringify(fieldStates ?? {})} />
  ),
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

const field: Field = {
  id: 'field-1',
  farmId: 'farm-1',
  name: 'Talhão 01',
  areaHa: 10,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

const inProgress: Operation = {
  id: 'op-1',
  fieldId: 'field-1',
  farmId: 'farm-1',
  type: 'SPRAYING',
  status: 'IN_PROGRESS',
  plannedStart: '2026-09-01T08:00:00Z',
  plannedEnd: '2026-09-01T12:00:00Z',
  actualStart: '2026-09-01T08:00:00Z',
  actualEnd: null,
  machineId: 'm1',
  pauseReason: null,
  itemId: null,
  itemQuantity: null,
  areaHa: null,
}

const ndviLayer: MapLayer = {
  id: 'layer-1',
  farmId: 'farm-1',
  fieldId: 'field-1',
  name: 'NDVI',
  kind: 'NDVI',
  source: 'demo',
  tileUrl: null,
  acquiredAt: null,
  status: 'READY',
}

function renderMap(seed: (client: QueryClient) => void) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  seed(client)
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/map']}>
        <MapPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('MapPage', () => {
  beforeEach(() => {
    fetchFieldsMock.mockReset()
    fetchOperationsMock.mockReset()
    fetchFieldsMock.mockResolvedValue([])
    fetchOperationsMock.mockResolvedValue([])
  })

  it('shows the status map and ignores map-layer data', async () => {
    renderMap((client) => {
      client.setQueryData(['fields', 'all'], [field])
      client.setQueryData(['operations', 'all'], [])
      client.setQueryData(['map', 'layers', 'all'], [ndviLayer])
    })
    expect(await screen.findByText('Mapa')).toBeInTheDocument()
    expect(screen.getByTestId('field-map')).toBeInTheDocument()
    expect(screen.getByText('Status dos talhões')).toBeInTheDocument()
    expect(screen.queryByText('Camadas')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'NDVI' })).not.toBeInTheDocument()
    expect(screen.queryByText('Nenhuma camada disponível')).not.toBeInTheDocument()
  })

  it('colors polygons from operations', async () => {
    renderMap((client) => {
      client.setQueryData(['fields', 'all'], [field])
      client.setQueryData(['operations', 'all'], [inProgress])
    })
    expect(await screen.findByTestId('field-map')).toBeInTheDocument()
    expect(screen.getByTestId('field-map').dataset.states).toBe(JSON.stringify({ 'field-1': 'progress' }))
  })

  it('stays busy while operations are still loading', async () => {
    fetchOperationsMock.mockImplementation(() => new Promise(() => {}))
    renderMap((client) => {
      client.setQueryData(['fields', 'all'], [field])
    })
    expect(await screen.findByRole('status', { busy: true })).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })

  it('fails the map when operations error instead of painting every field as none', async () => {
    fetchOperationsMock.mockRejectedValue(new ApiError('ops down', 500, 'cid-ops'))
    renderMap((client) => {
      client.setQueryData(['fields', 'all'], [field])
    })
    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar os dados')
    expect(screen.getByText('ops down')).toBeInTheDocument()
    expect(screen.getByText('cid-ops')).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })

  it('retries fields and operations after a fields error', async () => {
    const user = userEvent.setup()
    fetchFieldsMock.mockRejectedValue(new ApiError('fields down', 500, 'cid-fields'))
    renderMap(() => {})
    expect(await screen.findByText('fields down')).toBeInTheDocument()
    expect(screen.getByText('cid-fields')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
    const fieldsBefore = fetchFieldsMock.mock.calls.length
    const opsBefore = fetchOperationsMock.mock.calls.length
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(fetchFieldsMock.mock.calls.length).toBeGreaterThan(fieldsBefore)
    expect(fetchOperationsMock.mock.calls.length).toBeGreaterThan(opsBefore)
  })
})
