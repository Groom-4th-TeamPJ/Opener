import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { setAccessToken } from '@/store/token-store'
import { AuthData } from '@/types/auth'
import { QUERY_KEYS } from '@/constants/query-key'

type LoginBody = { email: string; password: string }

async function loginApi(body: LoginBody): Promise<AuthData> {
  return api<AuthData>('/login', {
    method: 'POST',
    body,
  })
}

export default function useLogin() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGIN,
    mutationFn: loginApi,
    onSuccess: ({ accessToken }) => {
      setAccessToken(accessToken)
    },
  })
}
