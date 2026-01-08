import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { QUERY_KEYS } from '@/constants/query-key'
import { API_PATHS } from '@/constants/api-path'

type LoginBody = { email: string; password: string }

async function loginApi(body: LoginBody) {
  return api(
    API_PATHS.AUTH.FORM_LOGIN,
    {
      method: 'POST',
      body,
    },
    false
  )
}

export default function useLogin() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGIN,
    mutationFn: loginApi,
  })
}
