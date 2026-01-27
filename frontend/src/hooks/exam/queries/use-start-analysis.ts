import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import { toast } from 'sonner'

interface StartAnalysisRequest {
  sessionId: number
  questionResultId: number
  questionId: number
}

async function startAnalysisApi(body: StartAnalysisRequest) {
  return api(API_PATHS.CHAT.ANALYSIS, { method: 'POST', body })
}

export function useStartAnalysis() {
  return useMutation({
    mutationFn: startAnalysisApi,
    onError: () => {
      toast.error('분석 요청에 실패했습니다. 다시 시도해주세요.', { duration: 3000 })
    },
  })
}
