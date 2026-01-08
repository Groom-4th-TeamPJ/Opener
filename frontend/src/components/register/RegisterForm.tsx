'use client'

import { FieldErrors, useForm } from 'react-hook-form'
import RegisterFormView from '@/components/register/RegisterFormView'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import useRegister from '@/hooks/auth/use-register'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { RegisterFormValues, Term } from '@/types/auth.types'

const PASSWORD_REGEX: RegExp = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*])[A-Za-z\d~!@#$%^&*]{8,20}$/

// TODO: 에러 텍스트 상수화 및 파일 분리
const registerSchema = z.object({
  name: z.string().trim().min(2, '2~12 자리로 입력해주세요.').max(12, '2~12 자리로 입력해주세요.'),
})

// TODO: 에러 텍스트 상수화 및 파일 분리
export const formRegisterSchema = registerSchema.extend({
  email: z.email('올바른 이메일 형식이 아닙니다.'),

  password: z
    .string()
    .regex(PASSWORD_REGEX, '영문, 숫자, 특수문자(~!@#$%^&*) 조합 8~20 자리로 입력해주세요.'),
})

interface RegisterFormProps {
  signupToken?: string
}

export default function RegisterForm({ signupToken }: RegisterFormProps) {
  const router = useRouter()
  // TODO: signupToken 존재 시 유효성 검증 및 decode 진행
  const DEFAULT_SET = useMemo(
    () =>
      signupToken
        ? {
            schema: registerSchema,
            // TODO: decode 후 사용자 이름 default 설정
            defaultValues: { name: '' },
          }
        : {
            schema: formRegisterSchema,
            defaultValues: { name: '', email: '', password: '' },
          },
    [signupToken]
  )
  const {
    control,
    handleSubmit,
    setFocus,
    formState: { errors, isSubmitting },
    clearErrors,
    reset,
  } = useForm<RegisterFormValues>({
    defaultValues: DEFAULT_SET.defaultValues,
    resolver: zodResolver(DEFAULT_SET.schema),
  })

  const { mutate: handleRegister, isPending } = useRegister()

  const [terms, setTerms] = useState<Term>({
    service: false,
    privacy: false,
    age: false,
  })

  const [termError, setTermError] = useState<string | null>(null)

  const agreed: boolean = useMemo(() => Object.values(terms).every(Boolean), [terms])

  const onSubmit = async (formValues: RegisterFormValues) => {
    const form = signupToken ? { signupToken, ...formValues } : formValues
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
      mode={signupToken ? 'oauth' : 'form'}
    />
  )
}
