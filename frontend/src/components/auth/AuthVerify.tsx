'use client'

import { useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { refreshOnce } from '@/utils/api'
import { toast } from 'sonner'

const PUBLIC_PATHS = ['/login', '/register']

export default function AuthVerifyHandler() {
  const router = useRouter()
  const searchParams = useSearchParams()

  useEffect(() => {
    const performRefresh = async () => {
      const isSuccess = await refreshOnce()

      if (isSuccess) {
        const callback = searchParams.get('callback')
        const decodedPath = callback ? decodeURIComponent(callback) : '/'

        // 역방향 가드 (성공했는데 가려는 곳이 로그인/회원가입인 경우)
        const isRedirectingToPublic = PUBLIC_PATHS.some(
          (path) => decodedPath === path || decodedPath.startsWith(`${path}/`)
        )

        router.replace(isRedirectingToPublic ? '/' : decodedPath)
      } else {
        // 갱신 실패 시 로그인 페이지로
        toast.error('세션이 만료되었습니다. 다시 로그인해주세요.', { duration: 3000 })
        router.replace('/login')
      }
    }

    performRefresh()
  }, [router, searchParams])

  return (
    <div className="text-center">
      <h1 className="text-xl font-bold text-text-primary">인증 정보를 확인 중</h1>
      <p className="mt-2 text-text-secondary text-sm">세션을 연결하고 있습니다.</p>
    </div>
  )
}
