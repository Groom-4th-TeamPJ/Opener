'use client'

import { useForm } from 'react-hook-form'
import LoginFormView from '@/components/login/LoginFormView'
import useLogin from '@/hooks/auth/use-login'
import { useRouter } from 'next/navigation'
import { LoginFormValues } from '@/types/auth.types'
import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { UiError } from '@/types/api.types'

const ERROR_MSG: string =
  '아이디 또는 비밀번호가 잘못되었습니다.\n아이디와 비밀번호를 정확히 입력해주세요.'

export default function LoginForm() {
  const [lockMessage, setLockMessage] = useState<string | null>(null)
  const router = useRouter()
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    clearErrors,
    setError,
    resetField,
  } = useForm<LoginFormValues>({ defaultValues: { email: '', password: '' } })

  const { mutateAsync: login } = useLogin()

  const onSubmit = async (form: LoginFormValues) => {
    clearErrors()
    if (!form.email || !form.password) {
      setError('root', { message: ERROR_MSG })
      return
    }
    try {
      await login(form, {
        onSuccess: () => router.replace('/'),
      })
    } catch (e: unknown) {
      const error = e as UiError
      const errorCode = error.errorCode
      if (errorCode === 'A_015') {
        const message = error.message || '5분 후 다시 시도해주세요.'
        setLockMessage(message)
      } else {
        setError('root', { message: ERROR_MSG })
      }

      resetField('password')
    }
  }

  const handleOauthLogin = () => {
    sessionStorage.setItem('pending_oauth', 'true')
  }

  useEffect(() => {
    if (!lockMessage) return

    // 정규식으로 숫자 추출
    const initialMins = parseInt(lockMessage.match(/\d+/)?.[0] || '5', 10)
    const expiryTime = Date.now() + initialMins * 60 * 1000

    const update = () => {
      const diff = expiryTime - Date.now()

      if (diff <= 0) {
        clearErrors('root')
        setLockMessage(null) // 메시지 초기화
      } else {
        const currentMins = Math.ceil(diff / (1000 * 60))
        setError('root', {
          type: 'server',
          message: `계정이 잠겼습니다. ${currentMins}분 후 다시 시도해주세요.`,
        })
      }
    }

    update()
    const timer = setInterval(update, 60_000)

    return () => clearInterval(timer)
  }, [lockMessage, setError, clearErrors])

  useEffect(() => {
    // 로그인 시도 기록 확인
    const pendingAuth = sessionStorage.getItem('pending_oauth')

    if (pendingAuth === 'true') {
      // 기록이 있다면, 사용자가 인증을 완료하지 않고 돌아온 것으로 간주
      const timeoutId = setTimeout(() => {
        toast.error('로그인이 중단되었습니다.')
        sessionStorage.removeItem('pending_oauth')
      }, 100)

      return () => clearTimeout(timeoutId)
    }
  }, [])

  return (
    <LoginFormView
      control={control}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting}
      isDisabled={!!lockMessage}
      handleOauthLogin={handleOauthLogin}
    />
  )
}
