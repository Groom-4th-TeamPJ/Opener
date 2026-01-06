'use client'

import { useForm } from 'react-hook-form'
import LoginFormView from '@/components/login/LoginFormView'

export type LoginFormValues = {
  email: string
  password: string
}

const ERROR_MSG: string =
  '아이디 또는 비밀번호가 잘못되었습니다.\n아이디와 비밀번호를 정확히 입력해주세요.'

export default function LoginForm() {
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    clearErrors,
    setError,
    resetField,
  } = useForm<LoginFormValues>({ defaultValues: { email: '', password: '' } })

  const onSubmit = async (form: LoginFormValues) => {
    clearErrors()
    if (!form.email || !form.password) {
      setError('root', { message: ERROR_MSG })
      return
    }
    try {
      // TODO: 추후 API 연동
    } catch {
      resetField('password')
      setError('root', { message: ERROR_MSG })
    }
  }

  const handleOAuth = () => {
    // TODO: OAuth 처리
  }

  return (
    <LoginFormView
      control={control}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting}
      onOAuthClick={handleOAuth}
    />
  )
}
