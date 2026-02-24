import { describe, test, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import { server } from '@/mocks/server'
import { loginHandlers } from '@/mocks/handlers/auth-handlers'
import { createTestQueryWrapper } from '@/mocks/utils/query-client-provider'
import useLogin from '@/hooks/auth/use-login'

describe('useLogin', () => {
  test('로그인 성공: null을 resolve한다', async () => {
    server.use(loginHandlers.use('200 - 로그인 성공'))

    const { result } = renderHook(() => useLogin(), {
      wrapper: createTestQueryWrapper(),
    })

    await expect(
      result.current.mutateAsync({ email: 'a@a.com', password: '1234' })
    ).resolves.toBeNull()
  })

  test('로그인 실패(401)', async () => {
    server.use(loginHandlers.use('401 - 로그인 실패'))

    const { result } = renderHook(() => useLogin(), {
      wrapper: createTestQueryWrapper(),
    })

    await expect(
      result.current.mutateAsync({ email: 'a@a.com', password: 'wrong' })
    ).rejects.toBeDefined()
  })
})
