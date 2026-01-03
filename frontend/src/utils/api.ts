export type ApiInit = Omit<RequestInit, 'headers' | 'method' | 'body'> & {
  headers?: Record<string, string>
  token?: string | null
  withCredentials?: boolean
}

type RequestMethod = 'GET' | 'POST'

const DEFAULT_INIT: RequestInit = {
  cache: 'no-store',
  next: { revalidate: 0 },
}

const BASE_URL: string = '/api'

export async function api<B = unknown>(
  path: string,
  options?: {
    method?: RequestMethod
    body?: B
    init?: ApiInit
  }
): Promise<Response> {
  const url = `${BASE_URL}${path}`
  const init = options?.init
  const method = options?.method ?? (options?.body !== undefined ? 'POST' : 'GET')

  const headers = new Headers(init?.headers)
  const hasBody = method === 'POST' && options?.body !== undefined

  if (hasBody && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (init?.token) headers.set('Authorization', `Bearer ${init.token}`)

  return fetch(url, {
    ...DEFAULT_INIT,
    ...init,
    method,
    headers,
    credentials: init?.withCredentials ? 'include' : 'same-origin',
    body: hasBody ? JSON.stringify(options!.body) : undefined,
  })
}
