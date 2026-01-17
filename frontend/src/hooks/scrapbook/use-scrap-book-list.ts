import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { ScrapBookListResponse } from '@/types/scrapbook.type'
import api from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

export function getScrapBookList(): Promise<ScrapBookListResponse | null> {
  return api<ScrapBookListResponse>(API_PATHS.SCRAPBOOK.LIST, { method: 'GET' })
}

export default function useScrapBookList() {
  return useQuery({
    queryKey: QUERY_KEYS.SCRAPBOOK.LIST,
    queryFn: getScrapBookList,
  })
}
