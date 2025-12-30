'use client'

import { useForm } from 'react-hook-form'
import LoginFormView from './LoginFormView'

export type LoginFormValues = {
  email: string
  password: string
}

const ERROR_MSG = '아이디 또는 비밀번호가 잘못되었습니다.\n아이디와 비밀번호를 정확히 입력해주세요.'

export default function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    clearErrors,
    setError,
    resetField,
  } = useForm({ defaultValues: { email: '', password: '' } })

  const onSubmit = async (form: LoginFormValues) => {
    clearErrors()
    if (!form.email || !form.password) {
      setError('root', { message: ERROR_MSG })
    }
    resetField('password')
    // TODO: 추후 API 연동
  }

  const handleOAuth = () => {
    // TODO: OAuth 처리
    console.log('Kakao OAuth')
  }

  return (
    <LoginFormView
      register={register}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting}
      onOAuthClick={handleOAuth}
    />
  )
}
