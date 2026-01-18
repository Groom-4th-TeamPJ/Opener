import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'

interface NavigationButtonProps {
  isLastQuestion: boolean
  onNext: () => void
}

export default function NavigationButton({ isLastQuestion, onNext }: NavigationButtonProps) {
  const { currentIndex, getQuestionState } = useExamStore()
  const examData = useCurrentExam()

  if (!examData) return null

  const { questions } = examData
  const question = questions[currentIndex]
  const { isSubmitted } = getQuestionState(question.questionId)

  return (
    <Button
      onClick={isSubmitted ? onNext : undefined}
      disabled={!isSubmitted}
      variant="default"
      size="lg"
      className={cn(
        'bg-white text-primary-600 hover:text-primary-500 hover:bg-neutral-50 active:text-primary-700 active:bg-neutral-100 whitespace-nowrap',
        !isSubmitted && 'cursor-not-allowed'
      )}
    >
      {isLastQuestion ? '학습종료' : '다음'}
    </Button>
  )
}
