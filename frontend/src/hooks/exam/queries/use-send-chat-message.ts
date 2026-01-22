import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { QUERY_KEYS } from '@/constants/query-key'
import { API_PATHS } from '@/constants/api-path'
import type { SendChatMessageRequest } from '@/types/exam'
import { toast } from 'sonner'

async function sendChatMessageApi(body: SendChatMessageRequest) {
  return api(API_PATHS.CHAT.MESSAGE, { method: 'POST', body })
}

export function useSendChatMessage() {
  return useMutation({
    mutationKey: QUERY_KEYS.EXAM.CHAT,
    mutationFn: sendChatMessageApi,
    onError: () => {
      toast.error('메시지 전송에 실패했습니다. 다시 시도해주세요.', { duration: 3000 })
    },
  })
}
