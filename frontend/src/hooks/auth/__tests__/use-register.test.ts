import { describe, expect, test } from 'vitest'
import { renderHook } from '@testing-library/react'
import { server } from '@/mocks/server'
import { signupHandlers } from '@/mocks/handlers/auth-handlers'
import { createTestQueryWrapper } from '@/mocks/utils/query-client-provider'
import useRegister from '@/hooks/auth/use-register'

describe('useRegister', () => {
  test('회원가입 성공: null을 resolve한다', async () => {
    server.use(signupHandlers.use('200 - 회원가입 성공'))

    const { result } = renderHook(() => useRegister(), {
      wrapper: createTestQueryWrapper(),
    })

    await expect(
      result.current.mutateAsync({ name: 'test', email: 'a@a.com', password: '1234' })
    ).resolves.toBeNull()
  })

  test('회원가입 실패(401)', async () => {
    server.use(signupHandlers.use('401 - 회원가입 실패 (중복/검증)'))

    const { result } = renderHook(() => useRegister(), {
      wrapper: createTestQueryWrapper(),
    })

    await expect(
      result.current.mutateAsync({ name: 'test', email: 'a@a.com', password: 'wrong' })
    ).rejects.toBeDefined()
  })
})
