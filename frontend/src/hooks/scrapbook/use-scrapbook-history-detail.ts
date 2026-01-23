import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { ScrapbookQuestion } from '@/types/exam'
import api from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

export function getScrapBookHistoryDetail(questionResultId: number) {
  return api<ScrapbookQuestion>(`${API_PATHS.SCRAPBOOK.DETAIL}/${questionResultId}`)
}

export default function useScrapBookHistoryDetail(questionResultId: number) {
  return useQuery({
    queryKey: [QUERY_KEYS.SCRAPBOOK.DETAIL, questionResultId],
    queryFn: () => getScrapBookHistoryDetail(questionResultId),
    enabled: Number.isFinite(questionResultId),
  })
}
