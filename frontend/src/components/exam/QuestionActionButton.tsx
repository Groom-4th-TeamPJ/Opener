import Button from '@/components/common/Button'
import { Sparkles } from 'lucide-react'

interface QuestionActionButtonProps {
  submitted: boolean
  selectedChoice: number | null
  frqAnswer: string
  questionType: 'MCQ' | 'FRQ'
  isAnalysisActive: boolean
  onSubmit: () => void
  onShowAnalysis: () => void
}

export default function QuestionActionButton({
  submitted,
  selectedChoice,
  frqAnswer,
  questionType,
  isAnalysisActive,
  onSubmit,
  onShowAnalysis,
}: QuestionActionButtonProps) {
  const isDisabled =
    questionType === 'MCQ' ? selectedChoice === null : frqAnswer.trim() === ''

  if (!submitted) {
    return (
      <Button
        variant="default"
        size="lg"
        widthFull
        className="rounded-xl"
        onClick={onSubmit}
        disabled={isDisabled}
      >
        제출하기
      </Button>
    )
  }

  return (
    <Button
      variant="default"
      size="lg"
      widthFull
      className="rounded-xl"
      onClick={onShowAnalysis}
      disabled={isAnalysisActive}
      leftIcon={<Sparkles className="w-5 h-5" />}
    >
      오프너 분석 보기
    </Button>
  )
}
