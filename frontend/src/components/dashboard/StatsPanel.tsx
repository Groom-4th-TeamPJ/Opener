import AccuracyCard from './AccuracyCard'
import StatCard from './StatCard'

type StatsPanelProps = {
  accuracyPercent: number
  correctCount: number
  studyTimeText: string
  solvedCount: number
}

export default function StatsPanel({
  accuracyPercent,
  correctCount,
  studyTimeText,
  solvedCount,
}: StatsPanelProps) {
  return (
    <div className="flex-2 sm:grid grid-cols-2 gap-4 lg:gap-6">
      <AccuracyCard percent={accuracyPercent} correctCount={correctCount} />

      <StatCard
        title="총 학습 시간"
        iconSrc="icons/mingcute_time_primary.svg"
        iconAlt="총 학습"
        value={studyTimeText}
        subText="꾸준히 학습을 이어오고 있어요!"
      />

      <StatCard
        title="총 풀이 문제"
        iconSrc="icons/book-question-mark_primary.svg"
        iconAlt="총 풀이"
        value={`${solvedCount}문제`}
        subText="다양한 유형을 충분히 연습했어요!"
      />
    </div>
  )
}
