import LoginErrorHandler from '@/components/login/LoginErrorHandler'
import LoginForm from '@/components/login/LoginForm'
import { Metadata } from 'next'
import Image from 'next/image'
import Link from 'next/link'

interface LoginProps {
  searchParams: Promise<{
    error?: string
  }>
}

export const metadata: Metadata = {
  title: '로그인',
  description: '오프너 로그인 페이지',
}

export default async function Login({ searchParams }: LoginProps) {
  const { error } = await searchParams
  return (
    <div className="min-h-screen flex flex-col justify-center">
      {error ? (
        <LoginErrorHandler error={error} />
      ) : (
        <main className="min-w-sm flex flex-col gap-4 items-center mx-auto py-4 px-8">
          <div className="relative w-full h-12 mb-6">
            <Image
              src={'/image/logo_h56_p.svg'}
              alt="오프너"
              fill
              className="object-contain"
              priority
              fetchPriority="high"
            />
          </div>
          <LoginForm />
          <span className="text-xs text-text-tertiary">
            계정이 없으신가요?{' '}
            <Link href={'/register'} className="underline">
              회원가입
            </Link>
          </span>
        </main>
      )}
    </div>
  )
}
