import type { Exam, Question, StopwatchRef } from '@/types/exam'
import { getLevelConfig } from '@/utils/exam-styles'
import Stopwatch from '@/components/shared/Stopwatch'

interface QuestionTitleProps {
  exam: Exam
  question: Question
  stopwatchRef?: React.Ref<StopwatchRef>
}

export default function QuestionTitle({ exam, question, stopwatchRef }: QuestionTitleProps) {
  const levelConfig = getLevelConfig(question.level)

  return (
    <div className="flex items-center gap-4">
      <span className="bg-primary-600 text-white text-lg font-bold px-4 py-2 rounded-xl">
        {question.order}
      </span>
      <div>
        <p className="text-[1.375rem] font-semibold">
          {exam.year}학년도 {exam.examType.name}
        </p>
        <p className="text-sm text-text-secondary">{question.order}번 문항</p>
      </div>
      <div className="ml-auto flex items-center gap-4">
        <span className={`font-medium px-3 py-1.5 rounded-full border ${levelConfig.className}`}>
          {levelConfig.label}
        </span>
        <Stopwatch ref={stopwatchRef} />
      </div>
    </div>
  )
}
