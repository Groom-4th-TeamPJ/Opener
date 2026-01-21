import { useExamResult } from '@/hooks/exam/queries/use-exam-result'
import Image from 'next/image'
import type { ResultData } from '@/types/exam'
import { formatTimeWithHours } from '@/utils/format'

const RESULT_MENU: { label: string; key: keyof ResultData }[] = [
  { label: '풀이 시간', key: 'totalTimeSpent' },
  { label: '정답 수', key: 'correctCount' },
  { label: '오답 수', key: 'incorrectCount' },
  { label: '오프너 분석 수', key: 'openerUsageCount' },
]

export default function ExamResultSummary() {
  const { data } = useExamResult()

  return (
    <>
      <div className="flex flex-col items-center gap-5 p-6">
        <Image src="/icons/partypopper.svg" alt={'학습 종료 축하 아이콘'} width={60} height={60} />
        <h1 className="text-2xl text-text-primary font-bold">모든 문제 풀이를 마쳤어요</h1>
        <span className="text-text-secondary">학습 결과를 한눈에 확인해보세요</span>
      </div>
      {RESULT_MENU.map(({ label, key }) => (
        <div
          key={key}
          className="flex justify-between items-center border-b p-4  border-neutral-200 font-bold h-12"
        >
          <div>{label}</div>
          <div>
            {key === 'totalTimeSpent'
              ? data?.[key] != null
                ? formatTimeWithHours(data[key])
                : '-'
              : (data?.[key] ?? '-')}
          </div>
        </div>
      ))}
    </>
  )
}
