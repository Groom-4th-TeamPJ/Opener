import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-key'
import { startExamApi } from './use-start-exam'
import { ExamRequestParams } from '@/types/exam'

export function useExamCurrent(params: ExamRequestParams) {
  return useQuery({
    queryKey: QUERY_KEYS.EXAM.CURRENT(params),
    queryFn: () => startExamApi(params),
    // 시험지 데이터는 세션 동안 변하지 않으므로 staleTime을 무한으로 설정
    staleTime: Infinity,
    // 파라미터가 모두 존재할 때만 쿼리 실행
    enabled: !!(params.examYear && params.category && params.examType),
  })
}
