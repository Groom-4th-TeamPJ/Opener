import { ModalContent } from '@/components/common/Modal'
import AISparklesIcon from '@/components/icons/AISparklesIcon'

export default function NewQuestionLoading() {
  return (
    <ModalContent className="flex flex-col justify-center items-center gap-5 h-full">
      <div>
        <AISparklesIcon className="w-24 h-24 animate-pulse-scale" />
      </div>
      <div className="flex flex-col items-center">
        <span>AI가 당신의 오답 논리를 바탕으로</span>
        <span>문제를 제작 중입니다.</span>
      </div>
    </ModalContent>
  )
}
