import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import { FieldErrors, UseFormRegister, UseFormWatch } from 'react-hook-form'
import { RegisterFormValues, Term } from '@/components/register/RegisterForm'
import TermsAgreementSection from '@/components/register/TermsAgreementSection'
import { Dispatch, SetStateAction } from 'react'
import PasswordInput from './PasswordInput'

interface LoginFormViewProps {
  register: UseFormRegister<RegisterFormValues>
  watch: UseFormWatch<RegisterFormValues>
  onSubmit: () => void
  errors: FieldErrors<RegisterFormValues>
  terms: Term
  setTerms: Dispatch<SetStateAction<Term>>
  agreed: boolean
  termError: string | null
  setTermError: Dispatch<SetStateAction<string | null>>
  isSubmitting: boolean
}

const MAX_NAME_LENGTH = 12

export default function RegisterFormView({
  register,
  watch,
  onSubmit,
  errors,
  terms,
  setTerms,
  agreed,
  termError,
  setTermError,
  isSubmitting,
}: LoginFormViewProps) {
  return (
    // TODO: 스타일 적용
    <form onSubmit={onSubmit} className="w-full flex flex-col gap-2">
      <Input
        id="name"
        type="text"
        label="이름"
        autoComplete="name"
        placeholder="이름(또는 닉네임)"
        helperText="2~12 자리"
        error={errors.name?.message}
        maxLength={MAX_NAME_LENGTH}
        {...register('name')}
      />
      <Input
        id="email"
        type="email"
        label="이메일"
        autoComplete="email"
        placeholder="user@example.com"
        error={errors.email?.message}
        {...register('email')}
      />
      <PasswordInput register={register} watch={watch} error={errors.password?.message} />
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
