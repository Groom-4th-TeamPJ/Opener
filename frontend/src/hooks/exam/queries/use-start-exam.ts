import { useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import type { ExamRequestParams, ExamResponse } from '@/types/exam'
import { QUERY_KEYS } from '@/constants/query-key'
import { toast } from 'sonner'

export async function startExamApi({ examYear, category, examType }: ExamRequestParams) {
  return api<ExamResponse>(
    `${API_PATHS.EXAM.ROOT}?examYear=${examYear}&category=${category}&examType=${examType}`,
    { method: 'GET' }
  )
}

export function useStartExam() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: startExamApi,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(QUERY_KEYS.EXAM.CURRENT(variables), data)
    },
    onError: () => {
      toast.error('문제를 불러오지 못했습니다. 다시 시도해주세요.', { duration: 3000 })
    },
  })
}
