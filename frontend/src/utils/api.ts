import { API_PATHS } from '@/constants/api-path'
import type { ApiEnvelope, ApiFail, UiError } from '@/types/api.types'
import { toast } from 'sonner'

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
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.deving.xyz/api'
// 동시 401에도 refresh 1번
let refreshPromise: Promise<boolean> | null = null
export const refreshOnce = () =>
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
): Promise<T | null> {
  try {
    const res = await api(path, options)
    if (!res.ok) throw res
    const json = await readJsonOrNull<T>(res)
    if (!json) return null
    if (json.status !== 'success') throw toUiError(json)
    return json.data
  } catch (e) {
    if (e instanceof Response) {
      const status = e.status

      // body 파싱, 없으면 fallback
      let fail: ApiFail | null = null
      const ct = e.headers.get('content-type')

      if (ct?.includes('application/json')) {
        try {
          fail = (await e.json()) as ApiFail
        } catch {
          fail = null
        }
      }

      if (!fail) {
        fail = {
          status: 'error',
          code: status,
          message: e.statusText || '서버 오류가 발생했습니다.',
          data: null,
          error: null,
        }
      }

      if (retry && status === 401) {
        if (!(await refreshOnce())) {
          if (typeof window !== 'undefined') {
            toast.error('세션이 만료되었습니다. 다시 로그인해주세요.', { duration: 3000 })
            setTimeout(() => window.location.replace('/login'), 800)
          }
          throw toUiError(fail)
        }
        return apiJson<T>(path, options, false)
      }
      throw toUiError(fail)
    }

    throw toUiError(e)
  }
}

async function readJsonOrNull<T>(res: Response): Promise<ApiEnvelope<T> | null> {
  const text = await res.text().catch(() => '')
  if (!text) return null
  return JSON.parse(text) as ApiEnvelope<T>
}

function toUiError(e: unknown): UiError {
  if (e && typeof e === 'object' && 'code' in e && 'status' in e && 'message' in e) {
    const fail = e as ApiFail
    return {
      code: fail.code,
      errorCode: fail.error?.code ?? null,
      message: fail.error?.reason ?? fail.message,
    }
  }

  // 일반 Error(네트워크 등)
  if (e instanceof Error) {
    return { code: 0, errorCode: 'CLIENT_ERROR', message: e.message }
  }

  // 나머지
  return { code: 0, errorCode: 'UNKNOWN', message: '알 수 없는 오류가 발생했습니다.' }
}
