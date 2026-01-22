import type { NewQuestionAnalysisProps } from '@/types/exam-variant'
import renderLatex from '@/utils/render-latex'

export default function NewQuestionAnalysis({
  analysis,
  isSubmitted,
  isCorrect,
}: NewQuestionAnalysisProps) {
  if (!isSubmitted || isCorrect) return null

  return (
    <div
      className="bg-neutral-50 rounded-lg p-4 text-sm leading-6"
      dangerouslySetInnerHTML={{ __html: renderLatex(analysis) }}
    />
  )
}
