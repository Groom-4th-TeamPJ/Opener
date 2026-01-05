import { CircleChevronDown, CircleX } from 'lucide-react'
import type { ResultFeedbackProps } from '@/types/exam-variant'

export default function ResultBanner({ isSubmitted, isCorrect }: ResultFeedbackProps) {
  if (!isSubmitted || isCorrect === null) return null
  return (
    <>
      {isCorrect ? (
        <div className="p-4 flex justify-center items-center bg-success-200 flex-1 gap-1 rounded-lg md:min-h-15 lg:min-h-20">
          <CircleChevronDown className="stroke-success-600" />
          <div className="text-success-600 font-bold">정답입니다.</div>
        </div>
      ) : (
        <div className="p-4 flex justify-center items-center  bg-danger-200 flex-1 gap-1 rounded-lg md:min-h-15 lg:min-h-20">
          <CircleX className="stroke-danger-600" />
          <div className="text-danger-600 font-bold">오답입니다.</div>
        </div>
      )}
    </>
  )
}
