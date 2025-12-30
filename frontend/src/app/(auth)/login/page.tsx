import LoginForm from '@/components/login/LoginForm'
import Link from 'next/link'

export default function Login() {
  return (
    <main className="min-h-screen flex flex-col justify-center">
      <section className="flex flex-col gap-2 items-center mx-auto py-4 px-8">
        {/* TODO: 로고 삽입 */}
        <h1 className="mb-10">오프너(로고)</h1>
        <LoginForm />
        {/* TODO: 회원가입 링크 설정 */}
        <span className="text-sm">
          계정이 없으신가요?{' '}
          <Link href={'/'} className="underline">
            회원가입
          </Link>
        </span>
      </section>
    </main>
  )
}
