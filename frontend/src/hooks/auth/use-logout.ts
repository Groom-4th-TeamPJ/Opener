import { QUERY_KEYS } from '@/constants/query-key'
import api from '@/utils/api'
import { clearAccessToken } from '@/store/token-store'
import { useMutation } from '@tanstack/react-query'

async function logoutApi() {
  return api(
    'logout',
    { method: 'POST', init: { withCredentials: 'include', auth: 'none' } },
    false
  )
}

export default function useLogout() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGOUT,
    mutationFn: logoutApi,
    onSettled: clearAccessToken,
  })
}
