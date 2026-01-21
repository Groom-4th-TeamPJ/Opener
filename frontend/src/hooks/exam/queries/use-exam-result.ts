import { useQuery } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { useExamStore } from '@/stores/use-exam-store'
import type { ResultData } from '@/types/exam'

async function getExamResultApi(examResultId: number) {
  return api<ResultData>(`${API_PATHS.EXAM.SUBMIT}/${examResultId}/summary`, { method: 'GET' })
}

export function useExamResult() {
  const { examResultId } = useExamStore()

  return useQuery({
    queryKey: QUERY_KEYS.EXAM.RESULT(examResultId!),
    queryFn: () => getExamResultApi(examResultId!),
    enabled: false,
  })
}
