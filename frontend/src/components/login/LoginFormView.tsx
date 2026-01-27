import Button from '@/components/common/Button'
import Image from 'next/image'
import { Control, FieldErrors } from 'react-hook-form'
import { LoginFormValues } from '@/types/auth.types'
import AuthInput from '@/components/auth/AuthInput'
import Link from 'next/link'
import cn from '@/utils/cn'
import { API_PATHS } from '@/constants/api-path'

interface LoginFormViewProps {
  control: Control<LoginFormValues>
  onSubmit: () => void
  errors: FieldErrors<LoginFormValues>
  isDisabled: boolean
  isSubmitting: boolean
  handleOauthLogin: () => void
}

const OAUTH_URL =
  (process.env.NEXT_PUBLIC_URL ?? 'https://opener.deving.xyz/api') + API_PATHS.AUTH.OAUTH_LOGIN

export default function LoginFormView({
  control,
  onSubmit,
  errors,
  isDisabled,
  isSubmitting,
  handleOauthLogin,
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
      <Button
        type="submit"
        className="w-full leading-0 mt-4"
        isLoading={isSubmitting}
        disabled={isDisabled}
      >
        로그인
      </Button>
      <Link
        href={OAUTH_URL}
        onClick={handleOauthLogin}
        className={cn(
          'inline-flex items-center justify-center gap-2',
          'rounded-lg cursor-pointer h-10 px-4',
          isSubmitting || isDisabled ? 'bg-[#E6CC00] pointer-events-none' : 'bg-[#fee500]',
          'leading-0 transition-colors duration-200 hover:bg-[#F2D700]'
        )}
        prefetch={false}
      >
        <Image src={'/icons/kakao.svg'} alt="카카오 로그인" width={18} height={18} />
        <span className="text-black/85">카카오 로그인</span>
      </Link>
    </form>
  )
}
