import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { queryClient } from '@/app/queryClient'
import { apiGet, refreshSessionNow } from '@/shared/api/client'
import { useAuthStore } from '@/shared/auth/store'

const session = {
  accessToken: 'access',
  refreshToken: 'refresh',
  role: 'ADMIN',
  userId: 'user-1',
  name: 'Test',
  email: 'test@example.com',
}

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function signIn() {
  useAuthStore.getState().setSession(session)
}

describe('refreshSession coordinator', () => {
  let refreshPosts = 0
  let refreshGate = deferred<Response>()

  beforeEach(() => {
    refreshPosts = 0
    refreshGate = deferred<Response>()
    signIn()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/api/v1/auth/refresh')) {
        refreshPosts += 1
        return refreshGate.promise
      }
      return jsonResponse(401, { message: 'unauth', code: 'UNAUTHORIZED' })
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    useAuthStore.getState().clearSession()
  })

  it('clears the session on a solo default logout when refresh fails', async () => {
    const clear = vi.spyOn(queryClient, 'clear')
    const pending = refreshSessionNow()
    await vi.waitFor(() => expect(refreshPosts).toBe(1))
    refreshGate.resolve(jsonResponse(401, { message: 'expired' }))
    await expect(pending).resolves.toBe(false)
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(clear).toHaveBeenCalled()
  })

  it('does not clearSession when a 401 and createFarm join a failed refresh', async () => {
    const clear = vi.spyOn(queryClient, 'clear')
    const unauthorized = apiGet('/api/v1/farms')
    await vi.waitFor(() => expect(refreshPosts).toBe(1))
    const createFarmRefresh = refreshSessionNow({ logoutOnFailure: false })
    refreshGate.resolve(jsonResponse(401, { message: 'expired' }))
    await expect(createFarmRefresh).resolves.toBe(false)
    await expect(unauthorized).rejects.toMatchObject({ status: 401 })
    expect(refreshPosts).toBe(1)
    expect(clear).not.toHaveBeenCalled()
    expect(useAuthStore.getState().accessToken).toBe('access')
  })

  it('does not logout when logoutOnFailure false then true join the same failed refresh', async () => {
    const clear = vi.spyOn(queryClient, 'clear')
    const first = refreshSessionNow({ logoutOnFailure: false })
    await vi.waitFor(() => expect(refreshPosts).toBe(1))
    const second = refreshSessionNow()
    refreshGate.resolve(jsonResponse(401, { message: 'expired' }))
    await expect(first).resolves.toBe(false)
    await expect(second).resolves.toBe(false)
    expect(refreshPosts).toBe(1)
    expect(clear).not.toHaveBeenCalled()
    expect(useAuthStore.getState().accessToken).toBe('access')
  })

  it('issues one /auth/refresh POST for a shared inflight', async () => {
    const first = refreshSessionNow({ logoutOnFailure: false })
    const second = refreshSessionNow()
    await vi.waitFor(() => expect(refreshPosts).toBe(1))
    refreshGate.resolve(jsonResponse(401, { message: 'expired' }))
    await expect(first).resolves.toBe(false)
    await expect(second).resolves.toBe(false)
    expect(refreshPosts).toBe(1)
  })

  it('consumes the refresh token before HTTP so a retry cannot reuse the jti', async () => {
    const first = refreshSessionNow({ logoutOnFailure: false })
    await vi.waitFor(() => expect(refreshPosts).toBe(1))
    expect(useAuthStore.getState().refreshToken).toBeNull()
    refreshGate.resolve(jsonResponse(401, { message: 'expired' }))
    await expect(first).resolves.toBe(false)
    expect(useAuthStore.getState().refreshToken).toBeNull()
    expect(useAuthStore.getState().accessToken).toBe('access')

    const second = refreshSessionNow({ logoutOnFailure: false })
    await expect(second).resolves.toBe(false)
    expect(refreshPosts).toBe(1)
  })
})
