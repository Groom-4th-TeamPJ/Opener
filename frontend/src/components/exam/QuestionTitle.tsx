import type { Exam, Question, StopwatchRef } from '@/types/exam'
import Stopwatch from '@/components/shared/Stopwatch'
import { Badge } from '@/components/common/Badge'

interface QuestionTitleProps {
  exam: Exam
  question: Question
  stopwatchRef?: React.Ref<StopwatchRef>
}

const getPointVariant = (point: number): 'success' | 'warning' | 'danger' => {
  if (point === 2) return 'success'
  if (point === 3) return 'warning'
  return 'danger'
}

export default function QuestionTitle({ exam, question, stopwatchRef }: QuestionTitleProps) {
  const pointVariant = getPointVariant(question.point)

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-6">
      <span className="bg-primary-600 text-white text-lg font-bold px-4 py-2 rounded-lg">
        {question.order}
      </span>
      <div className="flex flex-col justify-center gap-0.5">
        <p className="text-lg xl:text-[1.375rem] text-text-primary font-bold leading-tight">
          {exam.year}학년도 {exam.examType.name}
        </p>
        <p className="text-sm text-text-secondary leading-tight">{question.order}번 문항</p>
      </div>

      <div className="w-full xl:w-auto xl:ml-auto flex items-center gap-4">
        <Badge
          type="solid-pastel"
          size="md"
          variant={pointVariant}
          pill
          label={`${question.point}점`}
          className="rounded-sm xl:h-7 xl:px-4 lg:text-base"
        />
        <Stopwatch ref={stopwatchRef} />
      </div>
    </div>
  )
}
