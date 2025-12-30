'use client'

import Button from '@/components/common/Button'
import Input from '@/components/common/Input'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Image from 'next/image'
import cn from '@/utils/cn'

const loginSchema = z.object({
  email: z.email('이메일 형식이 올바르지 않습니다').trim().min(1, '이메일을 입력해주세요'),
  password: z.string().trim().min(1, '비밀번호를 입력해주세요'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export default function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    resetField,
  } = useForm({ resolver: zodResolver(loginSchema) })

  const onSubmit = (form: LoginFormValues) => {
    resetField('password')
    // TODO: 추후 API 연동
  }
  return (
    <section>
      {/* TODO: 스타일 적용 */}
      <form
        onSubmit={handleSubmit(onSubmit)}
        className="flex flex-col gap-2 [&>button:last-child]:mt-4 items-center"
      >
        <Input
          id="email"
          type="email"
          label="이메일"
          autoComplete="true"
          {...register('email')}
          error={errors.email?.message}
        />
        <Input
          id="password"
          type="password"
          label="비밀번호"
          {...register('password')}
          error={errors.password?.message}
        />
        <Button type="submit" className="w-full leading-0" isLoading={isSubmitting}>
          로그인
        </Button>
      </form>
      <button
        className={cn(
          'w-full h-10 px-4 flex bg-[#fee500] leading-0 mt-2',
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
    </section>
  )
}
