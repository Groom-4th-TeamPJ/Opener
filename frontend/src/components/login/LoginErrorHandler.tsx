'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'

export default function LoginErrorHandler({ error }: { error: string }) {
  const router = useRouter()

  useEffect(() => {
    // 사용자에게 에러 알림
    const errorMsg =
      error === 'access_denied'
        ? '로그인이 취소되었습니다.'
        : '인증 세션이 만료되었습니다. 다시 시도해주세요.'

    // URL 파라미터 제거
    // replace를 사용해 히스토리에 에러 URL을 남기지 않음
    const timer = setTimeout(() => {
      toast.error(errorMsg, { duration: 3000 })
      router.replace('/login')
    }, 800)

    return () => clearTimeout(timer)
  }, [error, router])

  return null
}
