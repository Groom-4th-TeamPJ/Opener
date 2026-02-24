'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { refreshOnce } from '@/utils/api'

const PUBLIC_PATHS = ['/login', '/register']

interface AuthVerifyHandlerProps {
  callback?: string
}

export default function AuthVerifyHandler({ callback }: AuthVerifyHandlerProps) {
  const router = useRouter()

  useEffect(() => {
    const performRefresh = async () => {
      const isSuccess = await refreshOnce()

      if (isSuccess) {
        const decodedPath = callback ? decodeURIComponent(callback) : '/'

        // 역방향 가드 (성공했는데 가려는 곳이 로그인/회원가입인 경우)
        const isRedirectingToPublic = PUBLIC_PATHS.some(
          (path) => decodedPath === path || decodedPath.startsWith(`${path}/`)
        )

        router.replace(isRedirectingToPublic ? '/' : decodedPath)
      } else {
        // 갱신 실패 시 로그인 페이지로
        router.replace('/login')
      }
    }

    performRefresh()
  }, [router, callback])

  return (
    <div className="text-center">
      <h1 className="text-xl font-bold text-text-primary">인증 정보를 확인 중</h1>
      <p className="mt-2 text-text-secondary text-sm">세션을 연결하고 있습니다.</p>
    </div>
  )
}
