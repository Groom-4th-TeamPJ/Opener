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
        'bg-white text-primary-600 hover:bg-primary-50 active:bg-primary-100 hover:border-primary-600 whitespace-nowrap',
        !submitted && 'cursor-not-allowed opacity-50'
      )}
    >
      {isLastQuestion ? '학습종료' : '다음'}
    </Button>
  )
}
