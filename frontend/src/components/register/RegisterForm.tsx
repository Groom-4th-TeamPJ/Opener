'use client'

import { FieldErrors, useForm } from 'react-hook-form'
import RegisterFormView from '@/components/register/RegisterFormView'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import useRegister from '@/hooks/auth/use-register'
import { useRouter } from 'next/navigation'
import { RegisterFormValues, SignupTokenFields, Term } from '@/types/auth.types'
import { UiError } from '@/types/api.types'
import { jwtDecode } from 'jwt-decode'
import { toast } from 'sonner'

const PASSWORD_REGEX: RegExp = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*])[A-Za-z\d~!@#$%^&*]{8,20}$/

// TODO: 에러 텍스트 상수화 및 파일 분리
const registerSchema = z.object({
  name: z.string().trim().min(2, '2~12 자리로 입력해주세요.').max(12, '2~12 자리로 입력해주세요.'),
})

// TODO: 에러 텍스트 상수화 및 파일 분리
const formRegisterSchema = registerSchema.extend({
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

  const decodedToken = useMemo(() => {
    if (!signupToken) return null
    try {
      return jwtDecode<SignupTokenFields>(signupToken)
    } catch {
      return null
    }
  }, [signupToken])

  useEffect(() => {
    if (signupToken && !decodedToken) {
      toast.error('유효하지 않은 접근입니다. 다시 시도해주세요.')
      router.replace('/login')
    }
  }, [decodedToken, signupToken, router])

  const DEFAULT_SET = useMemo(() => {
    if (decodedToken) {
      return {
        schema: registerSchema,
        defaultValues: { name: decodedToken.nickname.trim() ?? '' },
      }
    }
    return {
      schema: formRegisterSchema,
      defaultValues: { name: '', email: '', password: '' },
    }
  }, [decodedToken])

  const {
    control,
    handleSubmit,
    setFocus,
    formState: { errors, isSubmitting },
    setError,
    clearErrors,
  } = useForm<RegisterFormValues>({
    defaultValues: DEFAULT_SET.defaultValues,
    resolver: zodResolver(DEFAULT_SET.schema),
  })

  const { mutateAsync: register } = useRegister()

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
      await register(form, {
        onSuccess: () => router.replace('/'),
      })
    } catch (e: unknown) {
      // TODO: 에러 코드 상수화
      const error = e as UiError
      if (error.code === 500 && error.errorCode === 'S_001') {
        setError('email', { message: '이미 존재하는 계정입니다.' }, { shouldFocus: true })
      }
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
      isSubmitting={isSubmitting}
      mode={signupToken ? 'oauth' : 'form'}
    />
  )
}
