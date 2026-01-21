import Button from '@/components/common/Button'
import OutlineCanIcon from '@/components/icons/OutlineCanIcon'
import { QUERY_KEYS } from '@/constants/query-key'
import { useMutationState } from '@tanstack/react-query'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'
import { useSubmitResult } from '@/hooks/exam/queries/use-submit-answer'
import { useGenerateQuestion } from '@/hooks/exam/queries/use-generate-question'
import { toast } from 'sonner'

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
  const questionId = examData?.questions[currentIndex]?.questionId ?? 0
  const { data: submitResult } = useSubmitResult(questionId)
  const questionResultId = submitResult?.questionResultId ?? 0
  const { refetch, isFetching: isGenerating } = useGenerateQuestion({
    questionId,
    questionResultId,
  })

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
    const isDisabledVariation = isCorrect !== false || hasNewQuestion || !questionResultId

    const handleVariationClick = async () => {
      if (!questionResultId) return
      const { isSuccess } = await refetch()
      if (isSuccess) {
        onVariationClick()
      } else {
        toast.error('변형문제 생성에 실패했습니다. 다시 시도해주세요.', { duration: 3000 })
      }
    }

    return (
      <Button
        variant="default"
        size="lg"
        widthFull
        onClick={handleVariationClick}
        disabled={isDisabledVariation}
        isLoading={isGenerating}
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
