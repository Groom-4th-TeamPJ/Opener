import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import { ArrowRight } from 'lucide-react'

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
  if (isLastQuestion) {
    return (
      <Button
        onClick={submitted ? onNext : undefined}
        disabled={!submitted}
        variant="default"
        size="lg"
        className={cn('rounded-xl px-6 transition-all', !submitted && 'opacity-50 cursor-not-allowed')}
      >
        시험완료
      </Button>
    )
  }

  return (
    <Button
      onClick={submitted ? onNext : undefined}
      disabled={!submitted}
      variant="ghost"
      className={cn(
        'w-14 h-14 rounded-full flex items-center justify-center transition-all shadow-lg p-0',
        submitted
          ? 'bg-primary-600 text-white hover:bg-primary-700 hover:scale-105 cursor-pointer'
          : 'bg-foreground/10 text-foreground/40 opacity-50 cursor-not-allowed'
      )}
    >
      <ArrowRight className="w-6 h-6" />
    </Button>
  )
}
