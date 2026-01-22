import AuthVerifyHandler from '@/components/auth/AuthVerify'

interface VerifyPageProps {
  searchParams: Promise<{
    callback?: string
  }>
}

export default async function VerifyPage({ searchParams }: VerifyPageProps) {
  const { callback } = await searchParams
  return (
    <main className="flex h-screen items-center justify-center bg-gray-50">
      <AuthVerifyHandler callback={callback} />
    </main>
  )
}
