import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { FieldsPage } from '@/features/fields/pages/FieldsPage'
import { createField, fetchFields } from '@/features/fields/api'
import { fetchFarms } from '@/features/farms/api'
import { useAuthStore } from '@/shared/auth/store'
import type { Field } from '@/features/fields/types'
import type { Farm } from '@/shared/api/types'
import { useUiStore } from '@/shared/ui/uiStore'

vi.mock('@/features/fields/api', () => ({
  fetchFields: vi.fn(),
  createField: vi.fn(),
  patchField: vi.fn(),
}))

vi.mock('@/features/farms/api', () => ({
  fetchFarms: vi.fn(),
  createFarm: vi.fn(),
  patchFarm: vi.fn(),
}))

const fetchFieldsMock = vi.mocked(fetchFields)
const createFieldMock = vi.mocked(createField)
const fetchFarmsMock = vi.mocked(fetchFarms)

const farm: Farm = {
  id: 'farm-1',
  name: 'Fazenda Alfa',
  location: 'MT',
  areaHa: 100,
  timezone: 'America/Sao_Paulo',
}

const created: Field = {
  id: 'field-2',
  farmId: 'farm-1',
  name: 'Norte',
  areaHa: 12.5,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

function renderFields() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/fields']}>
        <FieldsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('FieldsPage', () => {
  beforeEach(() => {
    fetchFieldsMock.mockReset()
    createFieldMock.mockReset()
    fetchFarmsMock.mockReset()
    fetchFieldsMock.mockResolvedValue([])
    fetchFarmsMock.mockResolvedValue([farm])
    createFieldMock.mockResolvedValue(created)
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

  it('creates a field through the gateway client', async () => {
    const user = userEvent.setup()
    renderFields()
    await user.click(await screen.findByRole('button', { name: 'Novo' }))
    const farmSelect = await screen.findByLabelText('Fazenda')
    await waitFor(() => expect(farmSelect).toHaveValue('farm-1'))
    await user.type(screen.getByLabelText('Nome'), 'Norte')
    await user.type(screen.getByLabelText('Área (ha)'), '12.5')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(createFieldMock).toHaveBeenCalledTimes(1))
    expect(createFieldMock.mock.calls[0]?.[0]).toEqual(
      expect.objectContaining({
        farmId: 'farm-1',
        name: 'Norte',
        crop: 'SOY',
        variety: null,
        areaHa: 12.5,
      }),
    )
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })
})
