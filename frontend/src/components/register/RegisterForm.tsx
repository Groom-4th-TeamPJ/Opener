'use client'

import { FieldErrors, useForm } from 'react-hook-form'
import RegisterFormView from '@/components/register/RegisterFormView'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import useRegister from '@/hooks/auth/use-register'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'

const PASSWORD_REGEX: RegExp = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*])[A-Za-z\d~!@#$%^&*]{8,20}$/

// TODO: 에러 텍스트 상수화 및 파일 분리
export const registerSchema = z.object({
  name: z.string().trim().min(2, '2~12 자리로 입력해주세요.').max(12, '2~12 자리로 입력해주세요.'),

  email: z.email('올바른 이메일 형식이 아닙니다.'),

  password: z
    .string()
    .regex(PASSWORD_REGEX, '영문, 숫자, 특수문자 (~!@#$%^&*) 조합 8~20 자리로 입력해주세요.'),
})

export type RegisterFormValues = z.infer<typeof registerSchema>

export type TermKey = 'service' | 'privacy' | 'age'

export type Term = Record<TermKey, boolean>

export default function RegisterForm() {
  const router = useRouter()
  const {
    control,
    handleSubmit,
    setFocus,
    formState: { errors, isSubmitting },
    clearErrors,
    reset,
  } = useForm<RegisterFormValues>({
    defaultValues: { name: '', email: '', password: '' },
    resolver: zodResolver(registerSchema),
  })

  const { mutate: handleRegister, isPending } = useRegister()

  const [terms, setTerms] = useState<Term>({
    service: false,
    privacy: false,
    age: false,
  })

  const [termError, setTermError] = useState<string | null>(null)

  const agreed: boolean = useMemo(() => Object.values(terms).every(Boolean), [terms])

  const onSubmit = async (form: RegisterFormValues) => {
    clearErrors()
    if (!agreed) {
      setTermError('약관에 동의해주세요.')
    }
    try {
      // TODO: 추후 API 연동
      handleRegister(form, {
        onSuccess: () => router.replace('/'),
        onError: (e) => toast.error(e.message),
      })
    } catch {
      reset()
    }
  }

  // submit 실패 시 첫 에러로 포커스
  const onInvalid = (errs: FieldErrors<RegisterFormValues>) => {
    const firstKey = Object.keys(errs)[0] as keyof RegisterFormValues | undefined
    if (!firstKey) return
    if (!agreed) {
      setTermError('약관에 동의해주세요.')
    }
    setFocus(firstKey, { shouldSelect: true })

    // 스크롤
    requestAnimationFrame(() => {
      const el = document.querySelector<HTMLElement>(`[name="${String(firstKey)}"]`)
      el?.scrollIntoView({ block: 'center', behavior: 'smooth' })
    })
  }

  return (
    <RegisterFormView
      control={control}
      errors={errors}
      onSubmit={handleSubmit(onSubmit, onInvalid)}
      terms={terms}
      setTerms={setTerms}
      agreed={agreed}
      termError={termError}
      setTermError={setTermError}
      isSubmitting={isSubmitting || isPending}
    />
  )
}
