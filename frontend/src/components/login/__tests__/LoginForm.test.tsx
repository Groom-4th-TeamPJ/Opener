import React from 'react'
import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, cleanup, fireEvent, waitFor, act } from '@testing-library/react'
import { QueryClient } from '@tanstack/react-query'
import LoginForm from '@/components/login/LoginForm'
import { QUERY_KEYS } from '@/constants/query-key'
import type { LoginFormValues } from '@/types/auth.types'
import { server } from '@/mocks/server'
import { loginHandlers } from '@/mocks/handlers/auth-handlers'
import { createTestQueryWrapper } from '@/mocks/utils/query-client-provider'

describe('LoginForm', () => {
  type LoginFormViewProps = {
    control: unknown
    onSubmit: () => void
    errors: unknown
    isDisabled: boolean
    isSubmitting: boolean
    handleOauthLogin: () => void
  }

  let fetchSpy: ReturnType<typeof vi.spyOn>

  const mocks = vi.hoisted(() => {
    let latestViewProps: LoginFormViewProps | null = null
    let mockFormValues: LoginFormValues = {
      email: 'test@example.com',
      password: 'Test1234!',
    }

    return {
      replaceMock: vi.fn(),
      toastErrorMock: vi.fn(),
      setErrorMock: vi.fn(),
      clearErrorsMock: vi.fn(),
      resetFieldMock: vi.fn(),
      getLatestViewProps: () => latestViewProps,
      setLatestViewProps: (props: LoginFormViewProps) => {
        latestViewProps = props
      },
      resetLatestViewProps: () => {
        latestViewProps = null
      },
      getMockFormValues: () => mockFormValues,
      setMockFormValues: (values: LoginFormValues) => {
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

  vi.mock('react-hook-form', () => ({
    useForm: () => ({
      control: {},
      handleSubmit: (onValid: (values: LoginFormValues) => void) => () =>
        onValid(mocks.getMockFormValues()),
      formState: {
        errors: {},
        isSubmitting: false,
      },
      clearErrors: mocks.clearErrorsMock,
      setError: mocks.setErrorMock,
      resetField: mocks.resetFieldMock,
    }),
  }))

  vi.mock('@/components/login/LoginFormView', () => ({
    default: (props: LoginFormViewProps) => {
      mocks.setLatestViewProps(props)
      return (
        <button type="button" onClick={props.onSubmit}>
          submit
        </button>
      )
    },
  }))

  beforeEach(() => {
    mocks.resetLatestViewProps()
    mocks.setMockFormValues({
      email: 'test@example.com',
      password: 'Test1234!',
    })
    mocks.toastErrorMock.mockReset()
    mocks.replaceMock.mockReset()
    mocks.setErrorMock.mockReset()
    mocks.clearErrorsMock.mockReset()
    mocks.resetFieldMock.mockReset()
    fetchSpy = vi.spyOn(globalThis, 'fetch')
  })

  afterEach(() => {
    fetchSpy.mockRestore()
    cleanup()
    vi.clearAllMocks()
  })

  test('로그인 폼 미기입', async () => {
    mocks.setMockFormValues({ email: '', password: '' })
    const { getByText } = render(<LoginForm />, { wrapper: createTestQueryWrapper() })

    fireEvent.click(getByText('submit'))

    await waitFor(() => {
      expect(fetchSpy).not.toHaveBeenCalled()
      expect(mocks.setErrorMock).toHaveBeenCalledWith(
        'root',
        expect.objectContaining({ message: expect.stringContaining('아이디') })
      )
    })
  })

  test('로그인 성공', async () => {
    server.use(loginHandlers.use('200 - 로그인 성공'))

    const queryClient = new QueryClient()
    const setQueryDefaultsSpy = vi.spyOn(queryClient, 'setQueryDefaults')
    const { getByText } = render(<LoginForm />, { wrapper: createTestQueryWrapper(queryClient) })

    await act(async () => {
      fireEvent.click(getByText('submit'))
    })

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled()
      expect(setQueryDefaultsSpy).toHaveBeenCalledWith(QUERY_KEYS.USER.CAN, { enabled: true })
      expect(mocks.replaceMock).toHaveBeenCalledWith('/')
    })
  })

  test('로그인 실패 - 이메일/비밀번호 틀림', async () => {
    server.use(loginHandlers.use('401 - 로그인 실패'))
    const { getByText } = render(<LoginForm />, { wrapper: createTestQueryWrapper() })

    await act(async () => {
      fireEvent.click(getByText('submit'))
    })

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled()
      expect(mocks.setErrorMock).toHaveBeenCalledWith(
        'root',
        expect.objectContaining({ message: expect.stringContaining('아이디') })
      )
      expect(mocks.resetFieldMock).toHaveBeenCalledWith('password')
    })
  })

  test('로그인 5회 이상 실패 시 계정 잠금', async () => {
    server.use(loginHandlers.use('401 - 로그인 실패 (A_015)'))

    const { getByText } = render(<LoginForm />, { wrapper: createTestQueryWrapper() })

    await act(async () => {
      fireEvent.click(getByText('submit'))
    })

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled()
      expect(mocks.setErrorMock).toHaveBeenCalledWith(
        'root',
        expect.objectContaining({ message: expect.stringContaining('계정이 잠겼습니다') })
      )
      expect(mocks.resetFieldMock).toHaveBeenCalledWith('password')
    })
  })
})
