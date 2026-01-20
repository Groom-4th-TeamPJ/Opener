import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { ScrapBookListData } from '@/types/scrapbook-list.type'
import api from '@/utils/api'
import { useQuery } from '@tanstack/react-query'

export function getScrapBookList(examId: number, page: number): Promise<ScrapBookListData | null> {
  return api<ScrapBookListData>(`${API_PATHS.SCRAPBOOK.LIST}/${examId}?size=8&page=${page}`, {
    method: 'GET',
  })
}

export default function useScrapBookList({ examId, page }: { examId: number; page: number }) {
  return useQuery({
    queryKey: [QUERY_KEYS.SCRAPBOOK.LIST, examId, page],
    queryFn: () => getScrapBookList(examId, page),
    enabled: Number.isFinite(examId) && Number.isFinite(page),
    staleTime: 300000,
  })
}
