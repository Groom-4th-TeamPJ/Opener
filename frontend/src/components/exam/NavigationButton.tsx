import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import { ChevronRight } from 'lucide-react'

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
  const label = isLastQuestion ? '학습 종료' : '다음'

  return (
    <Button
      onClick={submitted ? onNext : undefined}
      disabled={!submitted}
      variant="ghost"
      className={cn(
        'w-36 h-36 px-0 bg-white rounded-full flex items-center justify-center shadow-1 transition-all',
        submitted
          ? 'text-primary-600 hover:bg-white'
          : 'bg-neutral-50 text-neutral-200 cursor-not-allowed'
      )}
    >
      <div className="flex items-center text-xl justify-center gap-1">
        <p className="font-bold">{label}</p>
        {!isLastQuestion && <ChevronRight className="-mr-2 -ml-1" />}
      </div>
    </Button>
  )
}
