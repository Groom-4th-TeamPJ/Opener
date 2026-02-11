import '@testing-library/jest-dom/vitest'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { EventSource } from 'eventsource'
import { server } from '@/mocks/server'
import { resetSSEMockConfig } from '@/mocks/handlers/sse'

// Node.js 환경에서 EventSource 폴리필 설정
Object.defineProperty(globalThis, 'EventSource', {
  value: EventSource,
  writable: true,
})

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'bypass' })
})

afterEach(() => {
  server.resetHandlers()
  resetSSEMockConfig()
})

afterAll(() => {
  server.close()
})
