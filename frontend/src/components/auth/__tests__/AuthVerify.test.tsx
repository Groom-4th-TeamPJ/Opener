import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, cleanup, waitFor } from '@testing-library/react'
import AuthVerifyHandler from '@/components/auth/AuthVerify'
import { refreshHandlers } from '@/mocks/handlers/auth-handlers'
import { server } from '@/mocks/server'

const mocks = vi.hoisted(() => ({
  replaceMock: vi.fn(),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}))

describe('AuthVerify', () => {
  beforeEach(() => {
    mocks.replaceMock.mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  test('RefreshToken이 유효하면 accessToken을 재발급받고 Callback Path로 이동', async () => {
    server.use(refreshHandlers.use('200 - refresh 성공'))

    render(<AuthVerifyHandler callback={encodeURIComponent('/')} />)

    await waitFor(() => {
      expect(mocks.replaceMock).toHaveBeenCalledWith('/')
    })
  })

  test('갱신 실패 시 로그인 페이지로 이동', async () => {
    server.use(refreshHandlers.use('401 - refresh 실패'))

    render(<AuthVerifyHandler callback={encodeURIComponent('/')} />)

    await waitFor(() => {
      expect(mocks.replaceMock).toHaveBeenCalledWith('/login')
    })
  })
})
