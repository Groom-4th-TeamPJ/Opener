import { useQuery } from '@tanstack/react-query'
import { setAccessToken, clearAccessToken } from '@/store/token-store'
import api from '@/utils/api'
import type { AuthData } from '@/types/auth'
import { QUERY_KEYS } from '@/constants/query-key'
import { useEffect } from 'react'

function postRefresh() {
  return api<AuthData>('/refresh', {
    method: 'POST',
    init: {
      withCredentials: 'include',
      auth: 'none',
    },
  })
}

export default function useRefreshOnBoot() {
  const query = useQuery({
    queryKey: QUERY_KEYS.AUTH.REFRESH,
    queryFn: postRefresh,
    staleTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
  })

  const { data, isError } = query

  useEffect(() => {
    if (data?.accessToken) {
      setAccessToken(data.accessToken)
    }
  }, [data])

  useEffect(() => {
    if (isError) {
      clearAccessToken()
    }
  }, [isError])

  return query
}
