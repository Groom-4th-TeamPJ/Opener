import { AuthData } from '@/types/auth'
import api from '../utils/api'

let accessToken: string | null = null
let refreshPromise: Promise<string> | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

export function clearAccessToken() {
  accessToken = null
}

export async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = api<AuthData>(
      '/refresh',
      {
        method: 'POST',
        init: { withCredentials: 'include', auth: 'none' }, // 쿠키만 전송, Authorization 스킵
      },
      false
    )
      .then(({ accessToken }) => {
        setAccessToken(accessToken)
        return accessToken
      })
      .catch((e: Error) => {
        clearAccessToken()
        throw e
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}
