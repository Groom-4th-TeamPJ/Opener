import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { QUERY_KEYS } from '@/constants/query-key'
import { API_PATHS } from '@/constants/api-path'

type RegisterBody = {
  email: string
  password: string
  name: string
}

async function registerApi(body: RegisterBody) {
  return api(API_PATHS.AUTH.FORM_REGISTER, {
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
