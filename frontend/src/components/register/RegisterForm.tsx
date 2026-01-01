'use client'

import { useForm } from 'react-hook-form'
import RegisterFormView from '@/components/register/RegisterFormView'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*])[A-Za-z\d~!@#$%^&*]{8,20}$/

export const registerSchema = z.object({
  name: z.string().trim().min(2, '2~12 자리로 입력해주세요.').max(12, '2~12 자리로 입력해주세요.'),

  email: z.email('올바른 이메일 형식 아닙니다.'),

  password: z
    .string()
    .regex(PASSWORD_REGEX, '영문, 숫자, 특수문자 (~!@#$%^&*) 조합 8~20 자리로 입력해주세요.'),
})

export type RegisterFormValues = z.infer<typeof registerSchema>

export type TermKey = 'tos' | 'privacy' | 'age'

export type Term = Record<TermKey, boolean>

export default function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    clearErrors,
    watch,
    reset,
  } = useForm({
    defaultValues: { name: '', email: '', password: '' },
    resolver: zodResolver(registerSchema),
  })

  const [terms, setTerms] = useState<Term>({
    tos: false,
    privacy: false,
    age: false,
  })

  const [termError, setTermError] = useState<string | null>(null)

  const agreed = useMemo(() => Object.values(terms).every(Boolean), [terms])

  const onSubmit = async (form: RegisterFormValues) => {
    clearErrors()
    if (!agreed) {
      setTermError('약관에 동의해주세요.')
    }
    try {
      // TODO: 추후 API 연동
    } catch {
      reset()
    }
  }

  return (
    <RegisterFormView
      register={register}
      watch={watch}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      terms={terms}
      setTerms={setTerms}
      agreed={agreed}
      termError={termError}
      setTermError={setTermError}
      isSubmitting={isSubmitting}
    />
  )
}
