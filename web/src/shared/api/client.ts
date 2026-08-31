import { useAuthStore } from '@/shared/auth/store'
import { CORRELATION_HEADER, newCorrelationId, newIdempotencyKey } from '@/shared/lib/correlation'

import { API_BASE, type ApiErrorBody, type TokenResponse } from './types'

export { API_BASE }

export class ApiError extends Error {
  readonly status: number
  readonly correlationId: string
  readonly code: string

  constructor(message: string, status: number, correlationId: string, code = 'REQUEST_FAILED') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.correlationId = correlationId
    this.code = code
  }
}

type QueryValue = string | number | boolean | undefined | null

type RequestOptions = {
  method?: string
  body?: unknown
  skipAuth?: boolean
  skipRefresh?: boolean
  idempotent?: boolean
  headers?: Record<string, string>
}

function toQuery(query?: Record<string, QueryValue>): string {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    params.set(key, String(value))
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

async function parseError(res: Response, correlationId: string): Promise<ApiError> {
  let body: ApiErrorBody | undefined
  try {
    body = (await res.json()) as ApiErrorBody
  } catch {
    body = undefined
  }
  return new ApiError(
    body?.message || res.statusText || 'Falha na requisição',
    res.status,
    body?.correlationId || correlationId,
    body?.code || 'REQUEST_FAILED',
  )
}

let inflightRefresh: Promise<boolean> | null = null

async function refreshSession(): Promise<boolean> {
  if (inflightRefresh) return inflightRefresh
  inflightRefresh = (async () => {
    const refreshToken = useAuthStore.getState().refreshToken
    if (!refreshToken) return false
    try {
      const data = await apiRequest<TokenResponse>('/api/v1/auth/refresh', {
        method: 'POST',
        body: { refreshToken },
        skipAuth: true,
        skipRefresh: true,
      })
      useAuthStore.getState().setSession(data)
      return true
    } catch {
      useAuthStore.getState().clearSession()
      return false
    }
  })()
  try {
    return await inflightRefresh
  } finally {
    inflightRefresh = null
  }
}

async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const correlationId = newCorrelationId()
  const headers: Record<string, string> = {
    Accept: 'application/json',
    [CORRELATION_HEADER]: correlationId,
    ...options.headers,
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (options.idempotent) {
    headers['Idempotency-Key'] = newIdempotencyKey()
  }
  const token = useAuthStore.getState().accessToken
  if (token && !options.skipAuth) {
    headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (res.status === 401 && !options.skipRefresh && !options.skipAuth) {
    const refreshed = await refreshSession()
    if (refreshed) {
      return apiRequest<T>(path, { ...options, skipRefresh: true })
    }
  }

  if (!res.ok) {
    throw await parseError(res, correlationId)
  }

  if (res.status === 204) {
    return undefined as T
  }

  const text = await res.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

export async function apiGet<T>(path: string, query?: Record<string, QueryValue>): Promise<T> {
  return apiRequest<T>(`${path}${toQuery(query)}`)
}

export async function apiPost<T>(path: string, body?: unknown, idempotent = true): Promise<T> {
  return apiRequest<T>(path, { method: 'POST', body, idempotent })
}

export async function apiDownload(path: string, filename: string): Promise<void> {
  const correlationId = newCorrelationId()
  const token = useAuthStore.getState().accessToken
  const headers: Record<string, string> = { [CORRELATION_HEADER]: correlationId }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, { headers })
  if (!res.ok) {
    throw await parseError(res, correlationId)
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
