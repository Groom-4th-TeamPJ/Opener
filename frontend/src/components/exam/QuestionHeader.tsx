import type { Exam, Question, StopwatchRef } from '@/types/exam'
import { getLevelConfig } from '@/utils/exam-styles'
import Stopwatch from '@/components/shared/Stopwatch'

interface QuestionHeaderProps {
  exam: Exam
  question: Question
  stopwatchRef?: React.Ref<StopwatchRef>
}

export default function QuestionHeader({ exam, question, stopwatchRef }: QuestionHeaderProps) {
  const levelConfig = getLevelConfig(question.level)

  return (
    <div className="flex items-center gap-4">
      <span className="bg-primary-600 text-white text-lg font-bold px-4 py-2 rounded-xl shadow-lg shadow-primary-600/20">
        {question.order}
      </span>
      <div>
        <p className="font-semibold">
          {exam.year}학년도 {exam.examType.name}
        </p>
        <p className="text-sm text-foreground/60">{question.order}번 문항</p>
      </div>
      <div className="ml-auto flex items-center gap-2">
        <span className={`text-xs font-medium px-3 py-1.5 rounded-full border ${levelConfig.className}`}>
          {levelConfig.label}
        </span>
        <Stopwatch ref={stopwatchRef} />
      </div>
    </div>
  )
}
