// 폴리필은 반드시 MSW 핸들러보다 먼저 import
import '@/mocks/polyfills'
import '@testing-library/jest-dom/vitest'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { server } from '@/mocks/server'
import { resetSSEMockConfig } from '@/mocks/handlers/sse'

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
