/* eslint-disable no-console */
import { useEffect } from 'react'
import { API_PATHS } from '@/constants/api-path'

interface UseSSEChatProps {
  sessionId: number
  enabled?: boolean
}

export function useSSEChat({ sessionId, enabled = true }: UseSSEChatProps): void {
  useEffect(() => {
    if (!enabled || sessionId === 0) return

    const BASE_URL = process.env.NEXT_PUBLIC_API_URL
    const SSE_URL = `${BASE_URL}${API_PATHS.CHAT.CONNECT}?sessionId=${sessionId}`

    // 표준 EventSource 사용
    const eventSource = new EventSource(SSE_URL)

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully')
    }

    eventSource.onmessage = (event) => {
      // 실시간 메시지 수신 시 처리 로직
      try {
        const data = JSON.parse(event.data)
        console.log('[SSE] New Message:', data)
      } catch {
        console.error('[SSE] Invalid JSON payload:', event.data)
      }
    }

    eventSource.onerror = (error) => {
      // 연결이 끊겨도 브라우저가 자동으로 재연결을 시도하므로 로그만 남김
      console.error('[SSE] Connection lost, browser will retry...', error)
    }

    return () => {
      console.log('[SSE] Cleanup: Closing connection')
      eventSource.close()
    }
  }, [sessionId, enabled])
}
