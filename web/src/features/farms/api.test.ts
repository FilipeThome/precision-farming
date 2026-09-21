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

import { useAuthStore } from '@/shared/auth/store'

import { createFarm } from './api'

const body = { name: 'Nova', location: 'MT', areaHa: 10, timezone: 'America/Cuiaba' }
const farm = { id: 'farm-1', name: 'Nova' }

function pendingStorageKey(userId = 'user-1') {
  return `pf:pending-farm:${userId}:${JSON.stringify({
    name: body.name,
    location: body.location,
    areaHa: body.areaHa,
    timezone: body.timezone,
  })}`
}

function signIn(userId = 'user-1') {
  useAuthStore.getState().setSession({
    accessToken: 'access',
    refreshToken: 'refresh',
    role: 'ADMIN',
    userId,
    name: 'Test',
    email: 'test@example.com',
  })
}

describe('createFarm', () => {
  beforeEach(() => {
    apiPost.mockReset()
    refreshSessionNow.mockReset()
    sessionStorage.clear()
    useAuthStore.getState().clearSession()
    signIn()
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
    await createFarm(body)
    expect(order).toEqual(['create', 'refresh'])
    expect(refreshSessionNow).toHaveBeenCalledTimes(1)
    expect(refreshSessionNow).toHaveBeenCalledWith({ logoutOnFailure: false })
  })

  it('does not retry a failed refresh (single-use refresh tokens)', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    expect(refreshSessionNow).toHaveBeenCalledTimes(1)
  })

  it('retries refresh without creating a second farm for the same payload', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
    expect(apiPost).toHaveBeenCalledTimes(1)
    expect(refreshSessionNow).toHaveBeenCalledTimes(2)
  })

  it('does not reuse another user pending farm after logout', async () => {
    apiPost.mockResolvedValueOnce({ id: 'farm-a', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })

    useAuthStore.getState().clearSession()
    signIn('user-2')
    apiPost.mockResolvedValueOnce({ id: 'farm-b', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-b' })
    expect(apiPost).toHaveBeenCalledTimes(2)
  })

  it('keeps a pending farm when a different payload is created successfully', async () => {
    const other = { name: 'Outra', location: 'GO', areaHa: 20, timezone: 'America/Sao_Paulo' }
    apiPost
      .mockResolvedValueOnce({ id: 'farm-a', name: 'Nova' })
      .mockResolvedValueOnce({ id: 'farm-b', name: 'Outra' })
    refreshSessionNow.mockResolvedValueOnce(false).mockResolvedValue(true)

    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    await expect(createFarm(other)).resolves.toMatchObject({ id: 'farm-b' })
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-a' })
    expect(apiPost).toHaveBeenCalledTimes(2)
  })

  it('persists a pending farm in sessionStorage when refresh fails', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    expect(sessionStorage.getItem(pendingStorageKey())).toBe(JSON.stringify(farm))
  })

  it('persists pending farm before refresh so a reload can skip POST', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockImplementation(async () => {
      expect(sessionStorage.getItem(pendingStorageKey())).toBe(JSON.stringify(farm))
      return true
    })
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
    expect(refreshSessionNow).toHaveBeenCalledTimes(1)
    expect(sessionStorage.getItem(pendingStorageKey())).toBeNull()
  })

  it('skips POST from sessionStorage after memory is cleared for the same user and payload', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    expect(apiPost).toHaveBeenCalledTimes(1)

    useAuthStore.getState().clearSession()
    signIn('user-2')
    signIn('user-1')
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
    expect(apiPost).toHaveBeenCalledTimes(1)
  })

  it('still POSTs for another user with the same payload', async () => {
    apiPost.mockResolvedValueOnce({ id: 'farm-a', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })

    useAuthStore.getState().clearSession()
    signIn('user-2')
    apiPost.mockResolvedValueOnce({ id: 'farm-b', name: 'Nova' })
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-b' })
    expect(apiPost).toHaveBeenCalledTimes(2)
  })

  it('deletes the sessionStorage key after a successful refresh', async () => {
    apiPost.mockResolvedValue(farm)
    refreshSessionNow.mockResolvedValueOnce(false)
    await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
    expect(sessionStorage.getItem(pendingStorageKey())).toBe(JSON.stringify(farm))
    refreshSessionNow.mockResolvedValue(true)
    await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
    expect(sessionStorage.getItem(pendingStorageKey())).toBeNull()
  })

  it('falls back to memory when sessionStorage.setItem throws', async () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota')
    })
    try {
      apiPost.mockResolvedValue(farm)
      refreshSessionNow.mockResolvedValue(false)
      await expect(createFarm(body)).rejects.toMatchObject({ code: 'SESSION_REFRESH_FAILED' })
      expect(apiPost).toHaveBeenCalledTimes(1)
      refreshSessionNow.mockResolvedValue(true)
      await expect(createFarm(body)).resolves.toMatchObject({ id: 'farm-1' })
      expect(apiPost).toHaveBeenCalledTimes(1)
    } finally {
      setItem.mockRestore()
    }
  })
})
