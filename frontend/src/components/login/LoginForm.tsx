'use client'

import { useForm } from 'react-hook-form'
import LoginFormView from '@/components/login/LoginFormView'
import useLogin from '@/hooks/auth/use-login'
import { useRouter } from 'next/navigation'
import { LoginFormValues } from '@/types/auth.types'

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

  const { mutate: login, isPending } = useLogin()

  const onSubmit = async (form: LoginFormValues) => {
    clearErrors()
    if (!form.email || !form.password) {
      setError('root', { message: ERROR_MSG })
      return
    }
    try {
      login(form, {
        onSuccess: () => router.replace('/'),
        onError: () => {
          setError('root', { message: ERROR_MSG })
          resetField('password')
        },
      })
    } catch {
      resetField('password')
      setError('root', { message: ERROR_MSG })
    }
  }

  return (
    <LoginFormView
      control={control}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting || isPending}
    />
  )
}
