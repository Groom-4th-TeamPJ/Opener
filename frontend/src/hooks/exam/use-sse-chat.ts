/* eslint-disable no-console */
import { useEffect, useRef } from 'react'
import { API_PATHS } from '@/constants/api-path'
import renderLatex from '@/utils/render-latex'

interface UseSSEChatProps {
  sessionId: number | null
  enabled?: boolean
  onChunk?: (chunk: string, displayMessage: string) => void
  onComplete?: (fullMessage: string) => void
}

// 수식 버퍼링 상태
interface LatexState {
  isInLatex: boolean
  type: 'block' | 'inline' | null
  buffer: string
}

export function useSSEChat({
  sessionId,
  enabled = true,
  onChunk,
  onComplete,
}: UseSSEChatProps): void {
  const fullMessageRef = useRef('')
  const displayMessageRef = useRef('')
  const pendingCharRef = useRef('')
  const latexStateRef = useRef<LatexState>({ isInLatex: false, type: null, buffer: '' })
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

    // 한 글자 처리 함수
    const processChar = (char: string) => {
      const state = latexStateRef.current
      const pending = pendingCharRef.current

      // 수식 안에 있는 경우
      if (state.isInLatex) {
        state.buffer += char

        // 블록 수식 종료 (\])
        if (state.type === 'block' && state.buffer.endsWith('\\]')) {
          const rendered = renderLatex(state.buffer, { blockDisplayMode: true })
          displayMessageRef.current += rendered
          onChunkRef.current?.(rendered, displayMessageRef.current)
          latexStateRef.current = { isInLatex: false, type: null, buffer: '' }
          return
        }

        // 인라인 수식 종료 (\))
        if (state.type === 'inline' && state.buffer.endsWith('\\)')) {
          const rendered = renderLatex(state.buffer, { blockDisplayMode: false })
          displayMessageRef.current += rendered
          onChunkRef.current?.(rendered, displayMessageRef.current)
          latexStateRef.current = { isInLatex: false, type: null, buffer: '' }
          return
        }

        // 수식 진행 중 - 출력 안 함
        return
      }

      // 수식 밖에서 \ 대기 중
      if (pending === '\\') {
        if (char === '[') {
          // 블록 수식 시작
          latexStateRef.current = { isInLatex: true, type: 'block', buffer: '\\[' }
          pendingCharRef.current = ''
          return
        } else if (char === '(') {
          // 인라인 수식 시작
          latexStateRef.current = { isInLatex: true, type: 'inline', buffer: '\\(' }
          pendingCharRef.current = ''
          return
        } else {
          // 일반 \ 문자 출력
          displayMessageRef.current += '\\' + char
          onChunkRef.current?.('\\' + char, displayMessageRef.current)
          pendingCharRef.current = ''
          return
        }
      }

      // \ 문자면 대기
      if (char === '\\') {
        pendingCharRef.current = '\\'
        return
      }

      // 일반 문자 출력
      displayMessageRef.current += char
      onChunkRef.current?.(char, displayMessageRef.current)
    }

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully, sessionId:', sessionId)
      fullMessageRef.current = ''
      displayMessageRef.current = ''
      pendingCharRef.current = ''
      latexStateRef.current = { isInLatex: false, type: null, buffer: '' }
    }

    // 'message' 이벤트: chunk 데이터 수신
    eventSource.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data) as { chunk: string }
        console.log('[SSE] Message:', data)

        fullMessageRef.current += data.chunk
        processChar(data.chunk)
      } catch (error) {
        console.error('[SSE] Error parsing message:', error, event.data)
      }
    })

    // 'complete' 이벤트: 스트리밍 완료
    eventSource.addEventListener('complete', (event) => {
      console.log('[SSE] Complete:', event.data)

      // 남은 pending 문자 처리
      if (pendingCharRef.current) {
        displayMessageRef.current += pendingCharRef.current
      }

      onCompleteRef.current?.(fullMessageRef.current)

      // 상태 초기화
      fullMessageRef.current = ''
      displayMessageRef.current = ''
      pendingCharRef.current = ''
      latexStateRef.current = { isInLatex: false, type: null, buffer: '' }
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
