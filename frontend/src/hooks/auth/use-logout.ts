import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import api from '@/utils/api'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'

async function logoutApi() {
  return api(API_PATHS.AUTH.LOGOUT, { method: 'POST' }, false)
}

export default function useLogout() {
  const router = useRouter()
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGOUT,
    mutationFn: logoutApi,
    onSettled: () => {
      queryClient.clear()
      router.replace('/login')
    },
  })
}
