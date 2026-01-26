import { useEffect, useRef } from 'react'
import { API_PATHS } from '@/constants/api-path'
import { useExamStore } from '@/stores/use-exam-store'
import { toast } from 'sonner'

interface UseSSEChatProps {
  sessionId: number | null
  questionId: number | null
  enabled?: boolean
}

export function useSSEChat({ sessionId, questionId, enabled = true }: UseSSEChatProps): void {
  const fullMessageRef = useRef('')
  const setStreamingMessage = useExamStore((state) => state.setStreamingMessage)
  const completeStreaming = useExamStore((state) => state.completeStreaming)

  // questionId를 ref로 관리하여 최신값 유지
  const questionIdRef = useRef(questionId)
  useEffect(() => {
    questionIdRef.current = questionId
  }, [questionId])

  useEffect(() => {
    if (!enabled || !sessionId) {
      return
    }

    const BASE_URL = process.env.NEXT_PUBLIC_API_URL
    const SSE_URL = `${BASE_URL}${API_PATHS.CHAT.CONNECT}?sessionId=${sessionId}`

    const eventSource = new EventSource(SSE_URL, { withCredentials: true })

    eventSource.onopen = () => {
      fullMessageRef.current = ''
    }

    // 'message' 이벤트: chunk 데이터 수신
    eventSource.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data) as { chunk: string }
        const currentStreaming =
          useExamStore.getState().questionStates[questionIdRef.current ?? 0]?.streamingMessage

        if (!currentStreaming && fullMessageRef.current !== '') {
          fullMessageRef.current = ''
        }

        fullMessageRef.current += data.chunk

        const currentQuestionId = questionIdRef.current
        if (currentQuestionId) {
          setStreamingMessage(currentQuestionId, fullMessageRef.current)
        }
      } catch (error) {
        console.error('[SSE] Error parsing message:', error, event.data)
      }
    })

    // 'complete' 이벤트: 스트리밍 완료
    eventSource.addEventListener('complete', () => {
      const currentQuestionId = questionIdRef.current
      if (currentQuestionId) {
        completeStreaming(currentQuestionId)
      }

      fullMessageRef.current = ''
    })

    // 'error' 이벤트: 서버에서 보내는 에러
    eventSource.addEventListener('error', (event) => {
      try {
        const data = JSON.parse((event as MessageEvent).data) as {
          type: string
          sessionId: string
          error: string
          errorCode: string
        }

        if (data.errorCode === 'N_001') {
          toast.error('캔이 부족합니다.', { duration: 3000 })
        } else {
          toast.error(data.error || '오류가 발생했습니다.', { duration: 3000 })
        }
      } catch {
        console.error('[SSE] Error parsing error event:', event)
      }
    })

    eventSource.onerror = (error) => {
      console.error('[SSE] Connection error:', error)
    }

    return () => {
      eventSource.close()
    }
  }, [sessionId, enabled, setStreamingMessage, completeStreaming])
}
