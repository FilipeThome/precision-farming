import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiPost = vi.fn()
const refreshSessionNow = vi.fn()

vi.mock('@/shared/api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/api/client')>()
  return {
    ...actual,
    apiGet: vi.fn(),
    apiPatch: vi.fn(),
    apiPost: (...args: unknown[]) => apiPost(...args),
    refreshSessionNow: (...args: unknown[]) => refreshSessionNow(...args),
  }
})

import { createFarm } from './api'

describe('createFarm', () => {
  beforeEach(() => {
    apiPost.mockReset()
    refreshSessionNow.mockReset()
  })

  it('refreshes the session after the farm is created', async () => {
    const order: string[] = []
    apiPost.mockImplementation(async () => {
      order.push('create')
      return { id: 'farm-1', name: 'Nova' }
    })
    refreshSessionNow.mockImplementation(async () => {
      order.push('refresh')
      return true
    })
    await createFarm({ name: 'Nova', location: 'MT', areaHa: 10, timezone: 'America/Cuiaba' })
    expect(order).toEqual(['create', 'refresh'])
    expect(refreshSessionNow).toHaveBeenCalledWith({ logoutOnFailure: false })
  })

  it('keeps the created farm error when refresh fails', async () => {
    apiPost.mockResolvedValue({ id: 'farm-1', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(false)
    await expect(
      createFarm({ name: 'Nova', location: 'MT', areaHa: 10, timezone: 'America/Cuiaba' }),
    ).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    expect(refreshSessionNow).toHaveBeenCalledTimes(3)
  })

  it('retries refresh without creating a second farm for the same payload', async () => {
    apiPost.mockResolvedValue({ id: 'farm-1', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(false)
    const body = { name: 'Retry', location: 'MT', areaHa: 10, timezone: 'America/Cuiaba' }
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
    expect(apiPost).toHaveBeenCalledTimes(1)
  })
})
