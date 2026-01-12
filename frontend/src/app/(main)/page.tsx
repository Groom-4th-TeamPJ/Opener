import GreetingSection from '@/components/dashboard/GreetingSection'
import StatsPanel from '@/components/dashboard/StatsPanel'
import StartSolveCard from '@/components/dashboard/StartSolveCard'

export default function HomePage() {
  // TODO: API 연동 예정
  const name = '홍길동'
  const hasStats = true

  const stats = {
    accuracyPercent: 75,
    correctCount: 106,
    studyTimeText: '3일 12시간 30분',
    solvedCount: 142,
  }

  return (
    <div className="min-h-[calc(100vh-4rem)] pt-10 sm:pt-0 flex items-center justify-center">
      <main className="w-full max-w-6xl px-4 mx-auto">
        <GreetingSection name={name} />

        <section className="lg:flex justify-center gap-6 space-y-4 lg:space-y-0">
          {hasStats && <StatsPanel {...stats} />}
          <StartSolveCard hasStats={hasStats} />
        </section>
      </main>
    </div>
  )
}
