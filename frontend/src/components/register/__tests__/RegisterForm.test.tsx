import React from 'react'
import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, cleanup, fireEvent, waitFor, act } from '@testing-library/react'
import { QueryClient } from '@tanstack/react-query'
import RegisterForm from '@/components/register/RegisterForm'
import { QUERY_KEYS } from '@/constants/query-key'
import type { RegisterFormValues, Term } from '@/types/auth.types'
import { server } from '@/mocks/server'
import { signupHandlers } from '@/mocks/handlers/auth-handlers'
import { createTestQueryWrapper } from '@/mocks/utils/query-client-provider'

type RegisterFormViewProps = {
  control: unknown
  errors: unknown
  onSubmit: () => void
  terms: Term
  setTerms: React.Dispatch<React.SetStateAction<Term>>
  agreed: boolean
  termError: string | null
  setTermError: React.Dispatch<React.SetStateAction<string | null>>
  isSubmitting: boolean
  mode: 'oauth' | 'form'
}

let fetchSpy: ReturnType<typeof vi.spyOn>

// 테스트 전용 Mock 함수 및 상태 관리
const mocks = vi.hoisted(() => {
  let latestViewProps: RegisterFormViewProps | null = null
  let mockFormValues: RegisterFormValues = {
    name: '홍길동',
    email: 'test@example.com',
    password: 'Test1234!',
  }

  return {
    replaceMock: vi.fn(),
    toastErrorMock: vi.fn(),
    setErrorMock: vi.fn(),
    clearErrorsMock: vi.fn(),
    setFocusMock: vi.fn(),
    jwtDecodeMock: vi.fn(),
    getLatestViewProps: () => latestViewProps,
    setLatestViewProps: (props: RegisterFormViewProps) => {
      latestViewProps = props
    },
    resetLatestViewProps: () => {
      latestViewProps = null
    },
    getMockFormValues: () => mockFormValues,
    setMockFormValues: (values: RegisterFormValues) => {
      mockFormValues = values
    },
  }
})

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}))

vi.mock('sonner', () => ({
  toast: { error: mocks.toastErrorMock },
}))

vi.mock('jwt-decode', () => ({
  jwtDecode: mocks.jwtDecodeMock,
}))

vi.mock('@/utils/error-handler', () => ({
  default: (code: string) => `message:${code}`,
}))

vi.mock('react-hook-form', () => ({
  useForm: () => ({
    control: {},
    handleSubmit: (onValid: (values: RegisterFormValues) => void) => () =>
      onValid(mocks.getMockFormValues()),
    setFocus: mocks.setFocusMock,
    setError: mocks.setErrorMock,
    clearErrors: mocks.clearErrorsMock,
    formState: {
      errors: {},
      isSubmitting: false,
    },
  }),
}))

vi.mock('@/components/register/RegisterFormView', () => ({
  default: (props: RegisterFormViewProps) => {
    mocks.setLatestViewProps(props)
    return (
      <button type="button" onClick={props.onSubmit}>
        submit
      </button>
    )
  },
}))

describe('RegisterForm', () => {
  beforeEach(() => {
    mocks.resetLatestViewProps()
    mocks.setMockFormValues({
      name: '홍길동',
      email: 'test@example.com',
      password: 'Test1234!',
    })
    mocks.toastErrorMock.mockReset()
    mocks.replaceMock.mockReset()
    mocks.setErrorMock.mockReset()
    mocks.clearErrorsMock.mockReset()
    mocks.setFocusMock.mockReset()
    mocks.jwtDecodeMock.mockReset()
    fetchSpy = vi.spyOn(globalThis, 'fetch')
  })

  afterEach(() => {
    fetchSpy.mockRestore()
    cleanup()
    vi.clearAllMocks()
  })

  test('잘못된 signupToken이면 에러 토스트 후 로그인으로 이동', async () => {
    mocks.jwtDecodeMock.mockImplementation(() => {
      throw new Error('invalid token')
    })

    render(<RegisterForm signupToken="bad-token" />, { wrapper: createTestQueryWrapper() })

    await waitFor(() => {
      expect(mocks.toastErrorMock).toHaveBeenCalled()
      expect(mocks.replaceMock).toHaveBeenCalledWith('/login')
    })
  })

  test('유효한 signupToken이면 pending_oauth를 제거', async () => {
    mocks.jwtDecodeMock.mockReturnValue({ sub: 'user' })
    const removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem')

    render(<RegisterForm signupToken="good-token" />, { wrapper: createTestQueryWrapper() })

    await waitFor(() => {
      expect(removeItemSpy).toHaveBeenCalledWith('pending_oauth')
    })
  })

  test('약관 미동의 상태에서 제출하면 약관 에러를 표시하고 회원가입 호출하지 않음', async () => {
    mocks.jwtDecodeMock.mockReturnValue(null)
    const { getByText } = render(<RegisterForm />, { wrapper: createTestQueryWrapper() })

    fireEvent.click(getByText('submit'))

    await waitFor(() => {
      expect(fetchSpy).not.toHaveBeenCalled()
      expect(mocks.getLatestViewProps()!.termError).toBe('약관에 동의해주세요.')
    })
  })

  test('약관 동의 후 제출하면 회원가입 성공 처리 및 메인으로 이동', async () => {
    mocks.jwtDecodeMock.mockReturnValue(null)
    server.use(signupHandlers.use('200 - 회원가입 성공'))

    const queryClient = new QueryClient()
    const setQueryDefaultsSpy = vi.spyOn(queryClient, 'setQueryDefaults')
    const { getByText } = render(<RegisterForm />, { wrapper: createTestQueryWrapper(queryClient) })

    await act(async () => {
      mocks.getLatestViewProps()!.setTerms({ service: true, privacy: true, age: true })
    })

    await waitFor(() => {
      expect(mocks.getLatestViewProps()!.agreed).toBe(true)
    })

    await act(async () => {
      fireEvent.click(getByText('submit'))
    })

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled()
      expect(setQueryDefaultsSpy).toHaveBeenCalledWith(QUERY_KEYS.USER.CAN, { enabled: true })
      expect(mocks.replaceMock).toHaveBeenCalledWith('/')
    })
  })

  test('회원가입 에러 코드 S_001이면 이메일 에러를 설정', async () => {
    mocks.jwtDecodeMock.mockReturnValue(null)
    server.use(signupHandlers.use('401 - 회원가입 실패 (중복/검증)'))

    const { getByText } = render(<RegisterForm />, { wrapper: createTestQueryWrapper() })

    await act(async () => {
      mocks.getLatestViewProps()!.setTerms({ service: true, privacy: true, age: true })
    })

    await waitFor(() => {
      expect(mocks.getLatestViewProps()!.agreed).toBe(true)
    })

    await act(async () => {
      fireEvent.click(getByText('submit'))
    })

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled()
      expect(mocks.setErrorMock).toHaveBeenCalledWith(
        'email',
        { message: '이미 존재하는 계정입니다.' },
        { shouldFocus: true }
      )
    })
  })
})
