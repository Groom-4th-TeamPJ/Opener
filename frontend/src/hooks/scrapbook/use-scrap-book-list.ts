import { QUERY_KEYS } from '@/constants/query-key'
import { ScrapBookListResponse } from '@/types/scrapbook.type'
import apiJson from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

export function getScrapBookList() {
  // TODO: 경로 API 주소 넣을 예정
  return apiJson<ScrapBookListResponse>('경로', { method: 'GET' })
}

export default function useScrapBookList() {
  return useQuery({
    queryKey: QUERY_KEYS.SCRAPBOOK.LIST,
    queryFn: getScrapBookList,
  })
}
