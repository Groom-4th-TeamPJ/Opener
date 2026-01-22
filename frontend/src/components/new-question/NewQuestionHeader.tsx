import Stopwatch from '@/components/shared/Stopwatch'
import type { StopwatchRef } from '@/types/exam'

interface NewQuestionHeaderProps {
  stopwatchRef?: React.RefObject<StopwatchRef | null>
}

export default function NewQuestionHeader({ stopwatchRef }: NewQuestionHeaderProps) {
  return (
    <div>
      <div className="flex justify-between">
        <h2 className="font-bold md:text-lg lg:text-xl">변형 문제</h2>
        <Stopwatch ref={stopwatchRef} />
      </div>
    </div>
  )
}
