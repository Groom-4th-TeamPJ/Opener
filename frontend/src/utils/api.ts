import { getAccessToken, refreshAccessToken } from '../store/token-store'
import type { ApiEnvelope } from '@/types/api.types'

type ApiError = Error & { status: number }

function fail(status: number, message?: string): never {
  const err = new Error(message ?? 'Api Error') as ApiError
  err.status = status
  throw err
}

type ApiInit = Omit<RequestInit, 'headers' | 'method' | 'body' | 'credentials'> & {
  headers?: Record<string, string>
  withCredentials?: RequestCredentials | boolean
  /** credentials는 include로 보내되 Authorization은 붙이지 않게(=refresh, login 등) */
  auth?: 'required' | 'none'
}

type RequestConfig<T> = {
  method?: RequestMethod
  body?: T
  init?: ApiInit
}

type RequestMethod = 'GET' | 'POST'

const DEFAULT_INIT: RequestInit = {
  cache: 'no-store',
  next: { revalidate: 0 },
}

const BASE_URL = process.env.VITE_API_URL ?? 'http://localhost:8080/api'

async function api<B = unknown>(path: string, options?: RequestConfig<B>): Promise<Response> {
  const url = `${BASE_URL}${path}`
  const init = options?.init
  const method = options?.method ?? (options?.body !== undefined ? 'POST' : 'GET')

  const headers = new Headers(init?.headers)
  const hasBody = method === 'POST' && options?.body !== undefined

  const credentials: RequestCredentials =
    typeof init?.withCredentials === 'boolean'
      ? init.withCredentials
        ? 'include'
        : 'same-origin'
      : (init?.withCredentials ?? 'same-origin')

  const authMode: 'required' | 'none' =
    init?.auth ?? (credentials === 'include' ? 'required' : 'none')

  if (hasBody && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  if (credentials === 'include' && authMode === 'required' && !headers.has('Authorization')) {
    const token = getAccessToken()
    if (!token) fail(401, 'Unauthorized')
    headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(url, {
    ...DEFAULT_INIT,
    ...init,
    method,
    headers,
    credentials,
    body: hasBody ? JSON.stringify(options!.body) : undefined,
  })

  if (!res.ok) fail(res.status, res.statusText)
  return res
}

async function requestJson<T>(path: string, options?: Parameters<typeof api>[1]): Promise<T> {
  const res = await api(path, options)
  const json = (await res.json()) as ApiEnvelope<T>

  if (json.status !== 'success') {
    fail(json.code, json.message)
  }
  return json.data
}

/**
 * @param retryAuth 기본 true
 * - 401이면 refresh 후 원요청 1회 재시도
 */
export default async function apiJson<T>(
  path: string,
  options?: Parameters<typeof api>[1],
  retry: boolean = true
): Promise<T> {
  try {
    return await requestJson<T>(path, options)
  } catch (e) {
    const status = e instanceof Error && 'status' in e ? (e as ApiError) : null
    const shouldRetry = retry && typeof status === 'number' && status === 401
    if (!shouldRetry) throw e

    // refresh 실패 시 최종 401
    try {
      await refreshAccessToken()
    } catch {
      fail(401, 'Unauthorized')
    }

    // 원요청 1회 재시도
    return requestJson<T>(path, options)
  }
}
