import Button from '@/components/common/Button'
import OutlineCanIcon from '@/components/icons/OutlineCanIcon'
import { QUERY_KEYS } from '@/constants/query-key'
import { useMutationState } from '@tanstack/react-query'

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
  const isSubmitting =
    useMutationState({
      filters: { mutationKey: QUERY_KEYS.EXAM.SUBMIT, status: 'pending' },
    }).length > 0

  const isDisabled = questionType === 'MCQ' ? selectedChoice === null : frqAnswer.trim() === ''

  if (!submitted) {
    return (
      <Button
        variant="default"
        size="lg"
        widthFull
        onClick={onSubmit}
        disabled={isDisabled}
        isLoading={isSubmitting}
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
      onClick={onShowAnalysis}
      leftIcon={<OutlineCanIcon />}
    >
      오프너 분석 보기
    </Button>
  )
}
