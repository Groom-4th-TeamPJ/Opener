import Button from '@/components/common/Button'
import OutlineCanIcon from '@/components/icons/OutlineCanIcon'
import { QUERY_KEYS } from '@/constants/query-key'
import { useMutationState } from '@tanstack/react-query'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'

interface QuestionActionButtonProps {
  onSubmit: () => void
  onShowAnalysis: () => void
  onVariationClick: () => void
}

export default function QuestionActionButton({
  onSubmit,
  onShowAnalysis,
  onVariationClick,
}: QuestionActionButtonProps) {
  const isSubmitting =
    useMutationState({
      filters: { mutationKey: QUERY_KEYS.EXAM.SUBMIT, status: 'pending' },
    }).length > 0
  const examData = useCurrentExam()
  const { currentIndex, getQuestionState } = useExamStore()

  if (!examData) return null

  const { questions } = examData
  const question = questions[currentIndex]
  const { selectedChoice, frqAnswer, isSubmitted, isAnalysisActive, isCorrect, hasNewQuestion } =
    getQuestionState(question.questionId)

  const isDisabled =
    question.questionType === 'MCQ' ? selectedChoice === null : frqAnswer.trim() === ''

  if (!isSubmitted) {
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
