import { useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-key'
import { useExamStore } from '@/stores/use-exam-store'
import type { ExamResponse } from '@/types/exam'

export default function useCurrentExam(): ExamResponse | null {
  const queryClient = useQueryClient()
  const examParams = useExamStore((s) => s.examParams)

  if (!examParams) return null

  const data = queryClient.getQueryData<ExamResponse>(QUERY_KEYS.EXAM.CURRENT(examParams))

  return data ?? null
}
