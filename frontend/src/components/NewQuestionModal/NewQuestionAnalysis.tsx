import type { NewQuestionAnalysisProps } from '@/types/exam-variant'

export default function NewQuestionAnalysis({
  analysis,
  isSubmitted,
  isCorrect,
}: NewQuestionAnalysisProps) {
  if (!isSubmitted || isCorrect) return null
  if (!analysis) {
    return <div className="bg-neutral-50 rounded-lg p-4 text-sm">해설을 불러오는 중입니다...</div>
  }

  return <div className="bg-neutral-50 rounded-lg p-4 text-sm">{analysis}</div>
}
