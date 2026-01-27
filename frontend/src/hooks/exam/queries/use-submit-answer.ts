import { useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/utils/api'
import { QUERY_KEYS } from '@/constants/query-key'
import type { SubmitAnswerRequest, SubmitAnswerResponse } from '@/types/exam'
import { API_PATHS } from '@/constants/api-path'
import { toast } from 'sonner'

interface SubmitAnswerParams {
  examResultId: number
  questionId: number
  body: SubmitAnswerRequest
}

async function submitAnswerApi({ examResultId, questionId, body }: SubmitAnswerParams) {
  return api<SubmitAnswerResponse>(
    `${API_PATHS.EXAM.SUBMIT}/${examResultId}/questions/${questionId}`,
    { method: 'POST', body }
  )
}

export function useSubmitAnswer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationKey: QUERY_KEYS.EXAM.SUBMIT,
    mutationFn: submitAnswerApi,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(QUERY_KEYS.EXAM.SUBMIT_RESULT(variables.questionId), data)
    },
    onError: () => {
      toast.error('오류가 발생했습니다. 다시 시도해주세요.', { duration: 3000 })
    },
  })
}

export function useSubmitResult(questionId: number) {
  const queryClient = useQueryClient()
  return queryClient.getQueryData<SubmitAnswerResponse>(QUERY_KEYS.EXAM.SUBMIT_RESULT(questionId))
}
