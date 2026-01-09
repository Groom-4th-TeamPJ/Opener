import { Card, CardContent } from '@/components/common/Card'
import LegendDot from './LegendDot'

// 너가 이미 만들어 둔 컴포넌트라고 가정
import CircularChart from './CircularChart'

type AccuracyCardProps = {
  percent: number
  correctCount: number
}

export default function AccuracyCard({ percent, correctCount }: AccuracyCardProps) {
  return (
    <Card className="lg:py-8 col-span-2">
      <CardContent className="flex flex-col items-center justify-center gap-6 md:flex-row md:gap-30">
        <div className="space-y-6">
          <div>
            <h1 className="text-lg lg:text-xl text-text-primary font-bold leading-tight">
              총 정답률
            </h1>
            <span className="text-xs lg:text-sm text-text-secondary">
              이번 달 학습 성취도 분석입니다.
            </span>
          </div>

          <div className="space-x-4">
            <LegendDot label="정답" className="before:bg-primary-600" />
            <LegendDot label="오답" className="before:bg-neutral-200" />
          </div>
        </div>

        <CircularChart percent={percent} label={`${correctCount}문제`} />
      </CardContent>
    </Card>
  )
}
