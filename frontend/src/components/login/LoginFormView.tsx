import Button from '@/components/common/Button'
import Image from 'next/image'
import { Control, FieldErrors } from 'react-hook-form'
import { LoginFormValues } from '@/components/login/LoginForm'
import AuthInput from '@/components/shared/AuthInput'

interface LoginFormViewProps {
  control: Control<LoginFormValues>
  onSubmit: () => void
  errors: FieldErrors<LoginFormValues>
  isSubmitting: boolean
  onOAuthClick: () => void
}

export default function LoginFormView({
  control,
  onSubmit,
  errors,
  isSubmitting,
  onOAuthClick,
}: LoginFormViewProps) {
  return (
    <form onSubmit={onSubmit} className="w-full flex flex-col gap-2">
      <AuthInput
        name="email"
        control={control}
        type="email"
        label="이메일"
        placeholder="이메일을 입력해주세요."
        size="lg"
        error={errors.root?.message ? ' ' : undefined}
      />
      <AuthInput
        name="password"
        control={control}
        type="password"
        label="비밀번호"
        placeholder="비밀번호를 입력해주세요."
        size="lg"
        error={errors.root?.message ? ' ' : undefined}
      />
      {/* 폼 상단 에러 */}
      {errors.root?.message ? (
        <p role="alert" className="text-xs text-red-600 whitespace-pre-line">
          {errors.root.message}
        </p>
      ) : null}
      <Button type="submit" className="w-full leading-0 mt-4" isLoading={isSubmitting}>
        로그인
      </Button>
      <Button
        onClick={onOAuthClick}
        className="bg-[#fee500] leading-0 transition-colors duration-200 hover:bg-[#F2D700] active:bg-[#E6CC00]"
        isLoading={isSubmitting}
      >
        <Image src={'/kakao/kakao.svg'} alt="카카오 로그인" width={18} height={18} />
        <span className="text-black/85">카카오 로그인</span>
      </Button>
    </form>
  )
}
