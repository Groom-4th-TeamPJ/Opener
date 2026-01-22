/* eslint-disable no-console */
import { useEffect, useRef } from 'react'
import { API_PATHS } from '@/constants/api-path'

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

    const eventSource = new EventSource(SSE_URL, { withCredentials: true })

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully, sessionId:', sessionId)
      messageBufferRef.current = ''
    }

    // 'message' 이벤트: chunk 데이터 수신
    eventSource.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data) as { chunk: string }
        console.log('[SSE] Message:', data)

        messageBufferRef.current += data.chunk
        onChunkRef.current?.(data.chunk, messageBufferRef.current)
      } catch (error) {
        console.error('[SSE] Error parsing message:', error, event.data)
      }
    })

    // 'complete' 이벤트: 스트리밍 완료
    eventSource.addEventListener('complete', (event) => {
      console.log('[SSE] Complete:', event.data)
      onCompleteRef.current?.(messageBufferRef.current)
      messageBufferRef.current = ''
    })

    eventSource.onerror = (error) => {
      console.error('[SSE] Connection error:', error)
    }

    return () => {
      console.log('[SSE] Cleanup: Closing connection')
      eventSource.close()
    }
  }, [sessionId, enabled])
}
