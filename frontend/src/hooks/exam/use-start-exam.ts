import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import type { ExamResponse } from '@/types/exam'

interface StartExamParams {
  examYear: number
  category: string
  examType: string
}

async function startExamApi({ examYear, category, examType }: StartExamParams) {
  return api<ExamResponse>(
    `${API_PATHS.EXAM.ROOT}?examYear=${examYear}&category=${category}&examType=${examType}`,
    { method: 'GET' }
  )
}

export default function useStartExam() {
  return useMutation({
    mutationFn: startExamApi,
  })
}
