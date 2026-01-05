import Button from '@/components/common/Button'
import { Control, FieldErrors } from 'react-hook-form'
import { RegisterFormValues, Term } from '@/components/register/RegisterForm'
import TermsAgreementSection from '@/components/register/TermsAgreementSection'
import { Dispatch, SetStateAction } from 'react'
import AuthInput from '@/components/shared/AuthInput'
import PasswordStrengthBadge from './PasswordStrengthBadge'

interface RegisterFormViewProps {
  control: Control<RegisterFormValues>
  errors: FieldErrors<RegisterFormValues>
  onSubmit: () => void
  terms: Term
  setTerms: Dispatch<SetStateAction<Term>>
  agreed: boolean
  termError: string | null
  setTermError: Dispatch<SetStateAction<string | null>>
  isSubmitting: boolean
}

const MAX_NAME_LENGTH = 12
const MAX_PASSWORD_LENGTH = 20

export default function RegisterFormView({
  control,
  errors,
  onSubmit,
  terms,
  setTerms,
  agreed,
  termError,
  setTermError,
  isSubmitting,
}: RegisterFormViewProps) {
  return (
    <form onSubmit={onSubmit} className="w-full flex flex-col gap-2">
      <AuthInput
        name="name"
        control={control}
        error={errors.name?.message}
        type="text"
        label="이름"
        placeholder="이름(또는 닉네임)"
        helperText="2~12 자리"
        maxLength={MAX_NAME_LENGTH}
        size="lg"
      />
      <AuthInput
        name="email"
        control={control}
        error={errors.email?.message}
        label="이메일"
        type="email"
        placeholder="이메일을 입력하세요"
        size="lg"
      />
      <AuthInput
        name="password"
        control={control}
        error={errors.password?.message}
        label="비밀번호"
        type="password"
        placeholder="비밀번호를 입력하세요"
        maxLength={MAX_PASSWORD_LENGTH}
        helperText="영문, 숫자, 특수문자 (~!@#$%^&*) 조합 8~20 자리"
        size="lg"
        badgeRenderer={(value) =>
          value.length > 0 ? <PasswordStrengthBadge value={value} /> : null
        }
      />
      <TermsAgreementSection
        agreed={agreed}
        terms={terms}
        setTerms={setTerms}
        termError={termError}
        setTermError={setTermError}
      />
      <Button type="submit" className="w-full leading-0 mt-4" isLoading={isSubmitting}>
        가입하기
      </Button>
    </form>
  )
}
