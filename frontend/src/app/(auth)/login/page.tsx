import LoginForm from '@/components/login/LoginForm'
import Image from 'next/image'
import Link from 'next/link'

export default function Login() {
  return (
    <div className="min-h-screen flex flex-col justify-center">
      <main className="min-w-sm flex flex-col gap-4 items-center mx-auto py-4 px-8">
        <div className="relative w-full h-10 mb-10">
          <Image src={'/image/logo_h56_p.svg'} alt="오프너" fill className="object-contain" />
        </div>
        <LoginForm />
        <span className="text-xs text-text-tertiary">
          계정이 없으신가요?{' '}
          <Link href={'/register'} className="underline">
            회원가입
          </Link>
        </span>
      </main>
    </div>
  )
}
