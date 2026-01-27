import ScrapbookCard from '@/components/scrapbook/ScrapbookCard'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '스크랩북',
  description: '저장한 문제와 오답을 시험별로 관리하세요',
}

export default function Scrapbook() {
  return (
    <div className="fixed inset-0 top-16 overflow-y-auto">
      <main className="w-full max-w-6xl p-16">
        <section className="mb-10">
          <h1 className="text-[1.75rem] font-bold text-text-primary">내 스크랩북</h1>
          <h2 className="text-sm sm:text-base text-text-secondary">
            지금까지 저장한 오프너 분석 내용을 확인할 수 있어요
          </h2>
        </section>
        <ScrapbookCard />
      </main>
    </div>
  )
}
