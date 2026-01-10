import cn from '@/utils/cn'
import Button from '@/components/common/Button'

interface NavigationButtonProps {
  isLastQuestion: boolean
  submitted: boolean
  onNext: () => void
}

export default function NavigationButton({
  isLastQuestion,
  submitted,
  onNext,
}: NavigationButtonProps) {
  return (
    <Button
      onClick={submitted ? onNext : undefined}
      disabled={!submitted}
      variant="default"
      size="lg"
      className={cn(
        'bg-white text-primary-600 hover:text-primary-500 hover:bg-neutral-50 active:text-primary-700 active:bg-neutral-100 whitespace-nowrap',
        !submitted && 'cursor-not-allowed'
      )}
    >
      {isLastQuestion ? '학습종료' : '다음'}
    </Button>
  )
}
