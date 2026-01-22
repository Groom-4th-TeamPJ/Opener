import AuthVerifyHandler from '@/components/auth/AuthVerify'

export default async function VerifyPage() {
  return (
    <main className="flex h-screen items-center justify-center bg-gray-50">
      <AuthVerifyHandler />
    </main>
  )
}
