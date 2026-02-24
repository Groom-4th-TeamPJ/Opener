import Button from '@/components/common/Button'
import OutlineCanIcon from '@/components/icons/OutlineCanIcon'
import { QUERY_KEYS } from '@/constants/query-key'
import { useMutationState, useQueryClient } from '@tanstack/react-query'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'
import { useSubmitResult } from '@/hooks/exam/queries/use-submit-answer'
import { useGenerateQuestion } from '@/hooks/exam/queries/use-generate-question'
import useCanCount from '@/hooks/header/use-can-count'
import { toast } from 'sonner'
import { gaEvent } from '@/utils/ga'

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
  const submitResult = useSubmitResult(questionId)
  const questionResultId = submitResult?.questionResultId ?? 0
  const { refetch } = useGenerateQuestion({
    questionId,
    questionResultId,
  })
  const { data: canData } = useCanCount()
  const currentCan = canData?.currentCan ?? 0
  const queryClient = useQueryClient()

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

    const handleVariationClick = () => {
      if (!questionResultId) return
      gaEvent('variation_cta_clicked', {
        question_id: question.questionId,
        question_type: question.questionType,
        question_category: question.category?.code,
        exam_year: examData.exam.examYear,
        exam_type: examData.exam.examType.code,
      })
      if (currentCan <= 0) {
        toast.error('캔이 부족합니다.', { duration: 3000 })
        return
      }
      onVariationClick()
      refetch().then((result) => {
        if (result.isSuccess) {
          queryClient.refetchQueries({ queryKey: QUERY_KEYS.USER.CAN })
        }
      })
    }

    return (
      <Button
        variant="default"
        size="lg"
        widthFull
        onClick={handleVariationClick}
        disabled={isDisabledVariation}
        leftIcon={<OutlineCanIcon />}
      >
        변형 문제 풀어보기
      </Button>
    )
  }

  const handleShowAnalysis = () => {
    if (currentCan <= 0) {
      toast.error('캔이 부족합니다.', { duration: 3000 })
      return
    }
    onShowAnalysis()
  }

  return (
    <Button
      variant="default"
      size="lg"
      widthFull
      onClick={handleShowAnalysis}
      leftIcon={<OutlineCanIcon />}
    >
      오프너 분석 보기
    </Button>
  )
}
