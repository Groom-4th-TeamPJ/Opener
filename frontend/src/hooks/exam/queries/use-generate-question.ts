import { useQuery } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import type { GenerateQuestionRequest, GenerateQuestionResponse } from '@/types/exam'

async function generateQuestionApi(body: GenerateQuestionRequest) {
  return api<GenerateQuestionResponse>(API_PATHS.CHAT.GENERATE, { method: 'POST', body })
}

export function useGenerateQuestion(params: GenerateQuestionRequest) {
  return useQuery({
    queryKey: QUERY_KEYS.EXAM.GENERATE(params.questionId),
    queryFn: () => generateQuestionApi(params),
    enabled: false,
    retry: false,
  })
}
