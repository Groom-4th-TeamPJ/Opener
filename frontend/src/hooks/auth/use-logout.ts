import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import api from '@/utils/api'
import { useMutation, useQueryClient } from '@tanstack/react-query'

async function logoutApi() {
  return api(API_PATHS.AUTH.LOGOUT, { method: 'POST' }, false)
}

export default function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGOUT,
    mutationFn: logoutApi,
    onSuccess: () => queryClient.clear(),
  })
}
