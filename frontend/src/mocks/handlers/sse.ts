import { http, HttpResponse } from 'msw'
import { API_PATHS } from '@/constants/api-path'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.ai.kr/api'

export interface SSEMockConfig {
  chunks: string[]
  delayMs: number
  shouldError?: boolean
  errorData?: {
    type: string
    sessionId: string
    error: string
    errorCode: string
  }
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

/**
 * SSE 스트림을 ReadableStream으로 생성
 */
function createSSEStream(config: SSEMockConfig): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder()
  const { chunks, delayMs, shouldError, errorData } = config

  return new ReadableStream({
    async start(controller) {
      try {
        if (shouldError && errorData) {
          await delay(delayMs)
          const errorEvent = `event: error\ndata: ${JSON.stringify(errorData)}\n\n`
          controller.enqueue(encoder.encode(errorEvent))
          controller.close()
          return
        }

        for (const chunk of chunks) {
          await delay(delayMs)
          const data = JSON.stringify({ chunk })
          const sseEvent = `event: message\ndata: ${data}\n\n`
          controller.enqueue(encoder.encode(sseEvent))
        }

        await delay(delayMs)
        const completeEvent = `event: complete\ndata: {}\n\n`
        controller.enqueue(encoder.encode(completeEvent))
        controller.close()
      } catch {
        controller.error(new Error('SSE stream error'))
      }
    },
  })
}

export const sseHandlers = [
  http.get(`${BASE_URL}${API_PATHS.CHAT.CONNECT}`, () => {
    const stream = createSSEStream(currentConfig)

    return new HttpResponse(stream, {
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        Connection: 'keep-alive',
      },
    })
  }),
]
