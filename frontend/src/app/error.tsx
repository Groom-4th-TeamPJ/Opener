'use client'

import Button from '@/components/common/Button'
import Header from '@/components/common/Header'
import Image from 'next/image'
import { useRouter } from 'next/navigation'
import { startTransition } from 'react'

interface ErrorProps {
  reset: () => void
}

export default function Error({ reset }: ErrorProps) {
  const router = useRouter()
  return (
    <>
      <Header />
      <div className="fixed inset-0 min-h-screen bg-neutral-50 flex items-center justify-center">
        <main className="relative pt-10 flex flex-col items-center text-center space-y-10">
          <div className="relative w-full h-30">
            <Image src={'icons/error.svg'} alt="에러 이미지" fill className="object-contain" />
          </div>

          <section className="space-y-2">
            <h1 className="text-2xl font-bold">일시적인 오류가 발생했어요</h1>
            <span className="text-text-secondary">잠시 후 다시 시도해 주세요</span>
          </section>
          <Button
            onClick={() => {
              startTransition(() => {
                router.refresh() // 서버 컴포넌트 재호출
                reset() // 에러 상태 초기화 및 컴포넌트 리렌더링 O, 서버 컴포넌트 실행 X
              })
            }}
            variant="default"
            size="md"
            className="bg-white text-primary-600 hover:text-primary-500 hover:bg-neutral-50 active:text-primary-700 active:bg-neutral-100 whitespace-nowrap"
          >
            새로 고침하기
          </Button>
        </main>
      </div>
    </>
  )
}
