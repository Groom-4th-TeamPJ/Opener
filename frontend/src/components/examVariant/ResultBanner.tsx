import { CircleChevronDown, CircleX } from 'lucide-react'
import { ModalContent } from '../common/Modal'

type ResultFeedbackProps = {
  isSubmitted: boolean
  isCorrect: boolean | null
}

export default function ResultBanner({ isSubmitted, isCorrect }: ResultFeedbackProps) {
  return (
    <>
      {isSubmitted && isCorrect && (
        <ModalContent className="flex px-4 py-0">
          <div className="p-4 flex justify-center bg-success-200 flex-1 gap-1 rounded-lg">
            <CircleChevronDown className="stroke-success-600" />
            <div className="text-success-600 font-bold">정답입니다.</div>
          </div>
        </ModalContent>
      )}
      {isSubmitted && !isCorrect && (
        <ModalContent className="flex px-4 py-0">
          <div className="p-4 flex justify-center bg-danger-200 flex-1 gap-1 rounded-lg">
            <CircleX className="stroke-danger-600" />
            <div className="text-danger-600 font-bold">오답입니다.</div>
          </div>
        </ModalContent>
      )}
    </>
  )
}
