'use client'

import { useForm } from 'react-hook-form'
import LoginFormView from '@/components/login/LoginFormView'
import useLogin from '@/hooks/auth/use-login'
import { useRouter } from 'next/navigation'
import { LoginFormValues } from '@/types/auth.types'
import { useEffect } from 'react'
import { toast } from 'sonner'

const ERROR_MSG: string =
  '아이디 또는 비밀번호가 잘못되었습니다.\n아이디와 비밀번호를 정확히 입력해주세요.'

// TODO: 사용자 계정 잠금 시 시간 및 안내 메시지 추가
export default function LoginForm() {
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
    } catch {
      resetField('password')
      setError('root', { message: ERROR_MSG })
    }
  }

  const handleOauthLogin = () => {
    sessionStorage.setItem('pending_oauth', 'true')
  }

  useEffect(() => {
    // 로그인 시도 기록 확인
    const pendingAuth = sessionStorage.getItem('pending_oauth')

    if (pendingAuth === 'true') {
      // 기록이 있다면, 사용자가 인증을 완료하지 않고 돌아온 것으로 간주
      setTimeout(() => {
        toast.error('로그인이 중단되었습니다.')
        sessionStorage.removeItem('pending_oauth')
      }, 100)
    }
  }, [])

  return (
    <LoginFormView
      control={control}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting}
      handleOauthLogin={handleOauthLogin}
    />
  )
}
