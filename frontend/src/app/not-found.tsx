import Header from '@/components/common/Header'
import { ROUTES } from '@/constants/routes'
import cn from '@/utils/cn'
import Image from 'next/image'
import Link from 'next/link'

export default function NotFound() {
  return (
    <>
      <Header />
      <div className="fixed inset-0 min-h-screen bg-neutral-50 flex items-center justify-center">
        <main className="relative pt-10 flex flex-col items-center text-center space-y-10">
          <div className="relative w-full h-30">
            <Image
              src={'icons/not-found.svg'}
              alt="찾을 수 없는 페이지"
              fill
              className="object-contain"
            />
          </div>

          <section className="space-y-2">
            <h1 className="text-2xl font-bold">찾을 수 없는 페이지예요</h1>
            <span className="text-text-secondary">주소를 다시 확인하거나 홈으로 이동해 주세요</span>
          </section>
          <Link
            href={ROUTES.DASHBOARD}
            className={cn(
              'h-10 px-4',
              'inline-flex items-center justify-center gap-2 rounded-lg cursor-pointer font-bold',
              'bg-white text-primary-600',
              'hover:text-primary-500 hover:bg-neutral-50 active:text-primary-700 active:bg-neutral-100 whitespace-nowrap'
            )}
          >
            대시보드로 이동하기
          </Link>
        </main>
      </div>
    </>
  )
}
