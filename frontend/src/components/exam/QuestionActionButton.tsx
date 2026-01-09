import Button from '@/components/common/Button'
import OutlineCanIcon from '@/components/icons/OutlineCanIcon'

interface QuestionActionButtonProps {
  submitted: boolean
  selectedChoice: number | null
  frqAnswer: string
  questionType: 'MCQ' | 'FRQ'
  isAnalysisActive: boolean
  isCorrect: boolean | null
  hasNewQuestion: boolean
  onSubmit: () => void
  onShowAnalysis: () => void
  onVariationClick: () => void
}

export default function QuestionActionButton({
  submitted,
  selectedChoice,
  frqAnswer,
  questionType,
  isAnalysisActive,
  isCorrect,
  hasNewQuestion,
  onSubmit,
  onShowAnalysis,
  onVariationClick,
}: QuestionActionButtonProps) {
  const isDisabled = questionType === 'MCQ' ? selectedChoice === null : frqAnswer.trim() === ''

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
        답안제출
      </Button>
    )
  }

  if (isAnalysisActive) {
    const isDisabledVariation = isCorrect !== false || hasNewQuestion

    return (
      <Button
        variant="default"
        size="lg"
        widthFull
        className="rounded-xl"
        onClick={onVariationClick}
        disabled={isDisabledVariation}
        leftIcon={<OutlineCanIcon />}
      >
        변형 문제 풀어보기
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
      leftIcon={<OutlineCanIcon />}
    >
      오프너 분석 보기
    </Button>
  )
}
