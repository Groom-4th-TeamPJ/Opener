/* eslint-disable no-console */
import { useState, useEffect, useRef } from 'react'
import { API_PATHS } from '@/constants/api-path'
import type { ChatMessage } from '@/types/exam'
import { formatChatTimestamp } from '@/utils/format'

interface UseSSEChatProps {
  sessionId: number
}

interface UseSSEChatReturn {
  messages: ChatMessage[]
  isConnected: boolean
  error: string | null
}

export function useSSEChat({ sessionId }: UseSSEChatProps): UseSSEChatReturn {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isConnected, setIsConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  const messageIdRef = useRef(1)
  const currentStreamingMessageRef = useRef<string>('')

  // SSE 연결 시작
  useEffect(() => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL
    const url = `${baseUrl}${API_PATHS.CHAT.CONNECT}?session=${sessionId}`

    console.log('[SSE] Connecting to:', url)

    // EventSource 생성
    const eventSource = new EventSource(url, {
      withCredentials: true,
    })

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully')
      setIsConnected(true)
      setError(null)
    }

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        // 서버에서 스트림 데이터를 받아서 메시지에 추가
        if (data.type === 'message') {
          currentStreamingMessageRef.current += data.content

          setMessages((prev) => {
            const lastMessage = prev[prev.length - 1]

            // 마지막 메시지가 assistant면 내용 업데이트
            if (lastMessage && lastMessage.role === 'assistant') {
              return [
                ...prev.slice(0, -1),
                {
                  ...lastMessage,
                  content: currentStreamingMessageRef.current,
                },
              ]
            } else {
              // 새 assistant 메시지 생성
              return [
                ...prev,
                {
                  id: messageIdRef.current++,
                  role: 'assistant',
                  content: currentStreamingMessageRef.current,
                  timestamp: formatChatTimestamp(new Date()),
                },
              ]
            }
          })
        } else if (data.type === 'done') {
          // 스트림 완료 - 다음 메시지를 위해 리셋
          currentStreamingMessageRef.current = ''
        }
      } catch (err) {
        console.error('Failed to parse SSE message:', err)
      }
    }

    eventSource.onerror = (err) => {
      console.error('[SSE] Error occurred:', err)
      console.error('[SSE] ReadyState:', eventSource.readyState)
      console.error('[SSE] URL:', url)
      setError('연결 중 오류가 발생했습니다.')
      setIsConnected(false)
      eventSource.close()
    }

    eventSourceRef.current = eventSource

    // Cleanup
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        setIsConnected(false)
      }
    }
  }, [sessionId])

  return {
    messages,
    isConnected,
    error,
  }
}
