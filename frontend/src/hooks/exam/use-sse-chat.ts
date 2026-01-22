/* eslint-disable no-console */
import { useEffect, useRef } from 'react'
import { API_PATHS } from '@/constants/api-path'

interface SSEChunkData {
  type: 'chunk' | 'done' | 'connected'
  sessionId: string
  chunk?: string
}

interface UseSSEChatProps {
  sessionId: number | null
  enabled?: boolean
  onChunk?: (chunk: string, fullMessage: string) => void
  onComplete?: (fullMessage: string) => void
}

export function useSSEChat({
  sessionId,
  enabled = true,
  onChunk,
  onComplete,
}: UseSSEChatProps): void {
  const messageBufferRef = useRef('')
  const onChunkRef = useRef(onChunk)
  const onCompleteRef = useRef(onComplete)

  // 콜백 참조 최신 상태로 유지
  useEffect(() => {
    onChunkRef.current = onChunk
    onCompleteRef.current = onComplete
  }, [onChunk, onComplete])

  useEffect(() => {
    if (!enabled || !sessionId) {
      return
    }

    const BASE_URL = process.env.NEXT_PUBLIC_API_URL
    const SSE_URL = `${BASE_URL}${API_PATHS.CHAT.CONNECT}?sessionId=${sessionId}`

    // 표준 EventSource 사용
    const eventSource = new EventSource(SSE_URL, { withCredentials: true })

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully, sessionId:', sessionId)
      messageBufferRef.current = ''
    }

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as SSEChunkData

        if (data.type === 'connected') {
          console.log('[SSE] Received First Message', data)
        }

        if (data.type === 'chunk' && data.chunk) {
          console.log('[SSE] AI New Message', data)

          messageBufferRef.current += data.chunk
          onChunkRef.current?.(data.chunk, messageBufferRef.current)
        } else if (data.type === 'done') {
          onCompleteRef.current?.(messageBufferRef.current)
          messageBufferRef.current = ''
        }
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
