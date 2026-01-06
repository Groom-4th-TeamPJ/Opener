import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { setAccessToken } from '@/store/token-store'
import { QUERY_KEYS } from '@/constants/query-key'
import { AuthData } from '@/types/auth'

type RegisterBody = {
  email: string
  password: string
  name: string
}

async function registerApi(body: RegisterBody): Promise<AuthData> {
  return api<AuthData>('/register', {
    method: 'POST',
    body,
  })
}

export default function useRegister() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.REGISTER,
    mutationFn: registerApi,
    onSuccess: ({ accessToken }) => {
      // 회원가입 후 즉시 로그인 상태로 전환
      setAccessToken(accessToken)
    },
  })
}
