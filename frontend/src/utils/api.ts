import { API_PATHS } from '@/constants/api-path'
import type { ApiEnvelope } from '@/types/api.types'
import { toast } from 'sonner'

type ApiError = Error & { status: number }

function fail(status: number, message?: string): never {
  const err = new Error(message ?? 'Api Error') as ApiError
  err.status = status
  throw err
}

type ApiInit = Omit<RequestInit, 'headers' | 'method' | 'body' | 'credentials'> & {
  headers?: Record<string, string>
  withCredentials?: RequestCredentials | boolean
}

type RequestConfig<T> = {
  method?: RequestMethod
  body?: T
  init?: ApiInit
}

type RequestMethod = 'GET' | 'POST'

const DEFAULT_INIT: RequestInit = { cache: 'no-store', next: { revalidate: 0 } }
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.deving.xyz/api/'

// 동시 401에도 refresh 1번
let refreshPromise: Promise<boolean> | null = null
const refreshOnce = () =>
  (refreshPromise ??= (async () => {
    try {
      const res = await fetch(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, {
        ...DEFAULT_INIT,
        method: 'POST',
        credentials: 'include',
      })
      return res.ok
    } finally {
      refreshPromise = null
    }
  })())

async function api<B = unknown>(path: string, options?: RequestConfig<B>): Promise<Response> {
  const init = options?.init
  const method = options?.method ?? (options?.body !== undefined ? 'POST' : 'GET')

  // 쿠키 기반: 기본 include
  const credentials: RequestCredentials =
    typeof init?.withCredentials === 'boolean'
      ? init.withCredentials
        ? 'include'
        : 'omit'
      : (init?.withCredentials ?? 'include')

  const headers = new Headers(init?.headers)

  // body 처리 (JSON / FormData / string / Blob)
  let body: BodyInit | undefined
  if (method === 'POST' && options?.body !== undefined) {
    const b = options.body as unknown
    if (b instanceof FormData) {
      headers.delete('Content-Type')
      body = b
    } else if (typeof b === 'string' || b instanceof Blob) {
      body = b
    } else {
      if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
      body = JSON.stringify(b)
    }
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...DEFAULT_INIT,
    ...init,
    method,
    headers,
    credentials,
    body,
  })

  if (!res.ok) fail(res.status, res.statusText)
  return res
}

/**
 * @param retry 기본 true
 * - 401이면 refresh 후 원요청 1회 재시도
 */
export default async function apiJson<T>(
  path: string,
  options?: Parameters<typeof api>[1],
  retry: boolean = true
): Promise<T> {
  try {
    const res = await api(path, options)
    const json = (await res.json()) as ApiEnvelope<T>
    if (json.status !== 'success') fail(json.code, json.message)
    return json.data
  } catch (e) {
    const status = e instanceof Error && 'status' in e ? (e as ApiError).status : null
    if (!(retry && status === 401)) throw e

    if (!(await refreshOnce())) {
      // 리프레시 토큰까지 만료된 최후의 상황
      if (typeof window !== 'undefined') {
        // 사용자에게 알림
        toast.error('세션이 만료되었습니다. 다시 로그인해주세요.', {
          duration: 3000, // 3초 유지
        })

        // 즉시 이동하지 않고 토스트를 볼 시간을 약간 주고 replace 실행
        // TODO: 에러 페이지 제작 후 적용
        setTimeout(() => {
          window.location.replace('/login')
        }, 800)
      }
      fail(401, 'Unauthorized')
    }

    // 원요청 1회 재시도
    const res = await api(path, options)
    const json = (await res.json()) as ApiEnvelope<T>
    if (json.status !== 'success') fail(json.code, json.message)
    return json.data
  }
}
