import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'

async function disconnectChatApi(sessionId: number) {
  return api(`${API_PATHS.CHAT.DISCONNECT}?sessionId=${sessionId}`, { method: 'POST' })
}

export function useDisconnectChat() {
  return useMutation({
    mutationFn: disconnectChatApi,
  })
}
