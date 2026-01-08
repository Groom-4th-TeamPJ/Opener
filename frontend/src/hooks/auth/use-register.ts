import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { QUERY_KEYS } from '@/constants/query-key'
import { API_PATHS } from '@/constants/api-path'

type RegisterBody = {
  email?: string
  password?: string
  name: string
  signupToken?: string
}

async function registerApi(body: RegisterBody) {
  const endPoint = body.signupToken ? API_PATHS.AUTH.OAUTH_REGISTER : API_PATHS.AUTH.FORM_REGISTER
  return api(endPoint, {
    method: 'POST',
    body,
  })
}

export default function useRegister() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.REGISTER,
    mutationFn: registerApi,
  })
}
