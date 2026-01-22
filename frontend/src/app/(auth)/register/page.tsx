import RegisterForm from '@/components/register/RegisterForm'
import Image from 'next/image'

interface RegisterProps {
  searchParams: Promise<{
    signupToken?: string
  }>
}

export default async function Register({ searchParams }: RegisterProps) {
  const { signupToken } = await searchParams

  return (
    <div className="min-h-screen flex flex-col justify-center">
      <main className="min-w-sm flex flex-col gap-4 items-center mx-auto py-4 px-8">
        <div className="relative w-full h-12 mb-6">
          <Image src={'/image/logo_h56_p.svg'} alt="오프너" fill className="object-contain" />
        </div>
        <RegisterForm signupToken={signupToken} />
      </main>
    </div>
  )
}
