import { API_PATHS } from '@/constants/api-path'
import { canCount } from '@/types/can'
import { QUERY_KEYS } from '@/constants/query-key'
import apiJson from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

export async function getCanCount() {
  return apiJson<canCount>(API_PATHS.USERS.ME, { method: 'GET' })
}

export default function useCanCount() {
  return useQuery({
    queryKey: QUERY_KEYS.USER.CAN,
    queryFn: getCanCount,
    staleTime: 1000 * 60 * 5, // 5분
  })
}
