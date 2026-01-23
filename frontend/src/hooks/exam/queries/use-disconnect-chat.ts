import { useMutation } from '@tanstack/react-query'
import api from '@/utils/api'
import { API_PATHS } from '@/constants/api-path'
import { toast } from 'sonner'

async function disconnectChatApi(sessionId: number) {
  return api(`${API_PATHS.CHAT.DISCONNECT}?sessionId=${sessionId}`, { method: 'POST' })
}

export function useDisconnectChat() {
  return useMutation({
    mutationFn: disconnectChatApi,
    onError: () => {
      toast.error('오류가 발생했습니다. 다시 시도해주세요.', { duration: 3000 })
    },
  })
}
