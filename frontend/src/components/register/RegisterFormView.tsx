import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import { FieldErrors, UseFormRegister } from 'react-hook-form'
import { RegisterFormValues } from '@/components/register/RegisterForm'
import TermsAgreementSection from './TermsAgreementSection'

interface LoginFormViewProps {
  register: UseFormRegister<RegisterFormValues>
  onSubmit: () => void
  errors: FieldErrors<RegisterFormValues>
  isSubmitting: boolean
}

export default function RegisterFormView({
  register,
  onSubmit,
  errors,
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
        {...register('name')}
      />
      <Input
        id="email"
        type="email"
        label="이메일"
        autoComplete="email"
        placeholder="user@example.com"
        {...register('email')}
      />
      <Input
        id="password"
        type="password"
        label="비밀번호"
        placeholder="•••••••••"
        helperText="영문, 숫자, 특수문자 (~!@#$%^&*) 조합 8~20 자리"
        {...register('password')}
      />
      <TermsAgreementSection />
      <Button type="submit" className="w-full leading-0 mt-4" isLoading={isSubmitting}>
        가입하기
      </Button>
    </form>
  )
}
