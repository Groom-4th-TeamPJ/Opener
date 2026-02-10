import { DashboardMetrics } from '@/types/dashboard.types'
import AccuracyCard from './AccuracyCard'
import StatCard from './StatCard'
import StartSolveCard from '../shared/StartSolveCard'

type StatsPanelProps = {
  metrics: DashboardMetrics | null
  isLoading: boolean
}

export default function StatsPanel({ metrics, isLoading }: StatsPanelProps) {
  if (isLoading) {
    return (
      <StartSolveCard
        title={<>대시보드를 불러오는 중이에요</>}
        description={<>잠시만 기다려 주세요.</>}
        label="불러오는 중..."
        disabled
        ctaContext="dashboard"
      />
    )
  } else if (!metrics) {
    return (
      <StartSolveCard
        title={
          <>
            지금 바로
            <span className="lg:block"> 문제를 풀어보세요</span>
          </>
        }
        description={
          <>
            과목과 시험을 선택하면
            <span className="lg:block"> 문제 풀이를 시작할 수 있어요</span>
          </>
        }
        label="문제풀이 시작하기"
        ctaContext="dashboard"
      />
    )
  }
  return (
    <>
      <div className="flex-2 sm:grid grid-cols-2 gap-4 lg:gap-6">
        <AccuracyCard
          percent={metrics.monthlyAverageCorrectRate}
          correctCount={metrics.monthlyQuestionsSolvedCount}
        />

        <StatCard
          title="총 학습 시간"
          iconSrc="icons/mingcute_time_primary.svg"
          iconAlt="총 학습"
          value={metrics.totalLearningTimeDesc}
          subText="꾸준히 학습을 이어오고 있어요!"
        />

        <StatCard
          title="총 풀이 문제"
          iconSrc="icons/book-question-mark_primary.svg"
          iconAlt="총 풀이"
          value={`${metrics.totalQuestionsSolvedCount}문제`}
          subText="다양한 유형을 충분히 연습했어요!"
        />
      </div>
      <StartSolveCard
        hasStats={true}
        title={
          <>
            지금 바로
            <span className="lg:block"> 문제를 풀어보세요</span>
          </>
        }
        description={
          <>
            과목과 시험을 선택하면
            <span className="lg:block"> 문제 풀이를 시작할 수 있어요</span>
          </>
        }
        label="문제풀이 시작하기"
        ctaContext="dashboard"
      />
    </>
  )
}
