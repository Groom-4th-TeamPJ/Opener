import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'

interface SaveChatMessageRequest {
  sessionId: number
  questionResultId: number
}

async function saveChatMessageApi(body: SaveChatMessageRequest) {
  return api(API_PATHS.CHAT.SAVE_CHAT, { method: 'POST', body })
}

export function useSaveChatMessage() {
  return useMutation({
    mutationFn: saveChatMessageApi,
  })
}
