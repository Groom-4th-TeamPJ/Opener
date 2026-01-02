import { Clock } from 'lucide-react'
import type { Exam, Question } from '@/types/exam'

interface QuestionHeaderProps {
  exam: Exam
  question: Question
  elapsedSeconds: number
}

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const getDifficultyConfig = (difficulty: 'EASY' | 'MEDIUM' | 'HARD') => {
  const configs = {
    EASY: {
      label: '쉬움',
      className: 'bg-warning-600/10 text-warning-600 border-warning-600/20',
    },
    MEDIUM: {
      label: '보통',
      className: 'bg-success-600/10 text-success-600 border-success-600/20',
    },
    HARD: {
      label: '어려움',
      className: 'bg-danger-600/10 text-danger-600 border-danger-600/20',
    },
  }
  return configs[difficulty]
}

export default function QuestionHeader({ exam, question, elapsedSeconds }: QuestionHeaderProps) {
  const difficultyConfig = getDifficultyConfig(question.difficulty)

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
        <span className={`text-xs font-medium px-3 py-1.5 rounded-full border ${difficultyConfig.className}`}>
          {difficultyConfig.label}
        </span>
        <div className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border bg-slate-100 text-slate-700 border-slate-200">
          <Clock className="w-3.5 h-3.5" />
          <span className="font-mono">{formatTime(elapsedSeconds)}</span>
        </div>
      </div>
    </div>
  )
}
