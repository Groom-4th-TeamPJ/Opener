import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { DashboardMetrics } from '@/types/dashboard.types'
import api from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

function getMetricsApi() {
  return api<DashboardMetrics>(API_PATHS.DASHBOARD.SUMMARY)
}

export default function useGetMetrics() {
  return useQuery({
    queryKey: QUERY_KEYS.DASHBOARD.SUMMARY,
    queryFn: getMetricsApi,
  })
}
