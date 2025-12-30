import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import Image from 'next/image'
import { FieldErrors, UseFormRegister } from 'react-hook-form'
import { LoginFormValues } from './LoginForm'
import cn from '@/utils/cn'

interface LoginFormViewProps {
  register: UseFormRegister<LoginFormValues>
  onSubmit: () => void
  errors: FieldErrors<LoginFormValues>
  isSubmitting: boolean
  onOAuthClick: () => void
}

export default function LoginFormView({
  register,
  onSubmit,
  errors,
  isSubmitting,
  onOAuthClick,
}: LoginFormViewProps) {
  return (
    // TODO: 스타일 적용
    <form onSubmit={onSubmit} className="flex flex-col gap-2">
      <Input id="email" type="email" label="이메일" autoComplete="email" {...register('email')} />
      <Input id="password" type="password" label="비밀번호" {...register('password')} />
      {/* 폼 상단 에러 */}
      {errors.root?.message ? (
        <p role="alert" className="text-xs text-red-600 whitespace-pre-line">
          {errors.root.message}
        </p>
      ) : null}
      <Button type="submit" className="w-full leading-0 mt-4" isLoading={isSubmitting}>
        로그인
      </Button>
      <button
        onClick={onOAuthClick}
        className={cn(
          'w-full h-10 px-4 flex bg-[#fee500] leading-0',
          'inline-flex items-center justify-center gap-2 rounded-xl font-semibold cursor-pointer',
          'transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
          'disabled:pointer-events-none disabled:opacity-50',
          'relative'
        )}
        disabled={isSubmitting}
      >
        <Image src={'/kakao/kakao.svg'} alt="카카오 로그인" width={18} height={18} />
        <span className="text-black/85">카카오 로그인</span>
      </button>
    </form>
  )
}
