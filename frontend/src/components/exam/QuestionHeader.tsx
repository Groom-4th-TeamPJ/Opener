import { Clock } from 'lucide-react'
import type { Exam, Question } from '@/types/exam'
import { formatTime } from '@/utils/format'
import { getLevelConfig } from '@/utils/exam-styles'

interface QuestionHeaderProps {
  exam: Exam
  question: Question
  elapsedSeconds: number
}

export default function QuestionHeader({ exam, question, elapsedSeconds }: QuestionHeaderProps) {
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
        <div className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border bg-slate-100 text-slate-700 border-slate-200">
          <Clock className="w-3.5 h-3.5" />
          <span className="font-mono">{formatTime(elapsedSeconds)}</span>
        </div>
      </div>
    </div>
  )
}
