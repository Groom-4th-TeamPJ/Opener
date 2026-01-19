import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { ScrapbookResponse } from '@/types/scrapbook.type'
import api from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

function getScrapbookCardApi() {
  return api<ScrapbookResponse>(API_PATHS.SCRAPBOOK.EXAM_LIST)
}

export default function useGetScrapbookCard() {
  return useQuery({
    queryKey: QUERY_KEYS.SCRAPBOOK.ROOT,
    queryFn: getScrapbookCardApi,
  })
}
