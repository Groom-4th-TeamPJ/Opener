import { sse } from 'msw'
import { API_PATHS } from '@/constants/api-path'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.ai.kr/api'

/**
 * SSE 이벤트 타입 정의
 * - message: AI 응답 청크
 * - complete: 스트리밍 완료
 * - error: 에러 발생
 */
type SSEEventMap = {
  message: { chunk: string }
  complete: Record<string, never>
  error: {
    type: string
    sessionId: string
    error: string
    errorCode: string
  }
}

export interface SSEMockConfig {
  chunks: string[]
  delayMs: number
  shouldError?: boolean
  errorData?: SSEEventMap['error']
}

const defaultConfig: SSEMockConfig = {
  chunks: ['Hello', ', ', 'World', '!'],
  delayMs: 50,
}

let currentConfig: SSEMockConfig = { ...defaultConfig }

export function setSSEMockConfig(config: Partial<SSEMockConfig>) {
  currentConfig = { ...defaultConfig, ...config }
}

export function resetSSEMockConfig() {
  currentConfig = { ...defaultConfig }
}

export function getSSEMockConfig() {
  return { ...currentConfig }
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export const sseHandlers = [
  sse<SSEEventMap>(`${BASE_URL}${API_PATHS.CHAT.CONNECT}`, async ({ client }) => {
    const { chunks, delayMs, shouldError, errorData } = currentConfig

    if (shouldError && errorData) {
      await delay(delayMs)
      client.send({
        event: 'error',
        data: errorData,
      })
      client.close()
      return
    }

    for (const chunk of chunks) {
      await delay(delayMs)
      client.send({
        event: 'message',
        data: { chunk },
      })
    }

    await delay(delayMs)
    client.send({
      event: 'complete',
      data: {},
    })
  }),
]
