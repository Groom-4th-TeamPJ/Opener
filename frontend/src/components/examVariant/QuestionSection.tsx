import { Timer } from 'lucide-react'
import { ModalContent, ModalHeader } from '../common/Modal'

type QuestionSectionProps = {
  passage: string
  onClose: () => void
}
export default function QuestionSection({ passage, onClose }: QuestionSectionProps) {
  return (
    <div>
      <ModalHeader closable={true} onClose={onClose} className="flex">
        <div className="flex justify-between">
          <h2 className="font-bold text-lg">변형 문제</h2>
          <div className="flex gap-1">
            <Timer width={24} height={24} className="stroke-Neutral-600" />
            <span>{/* 스탑워치 */}</span>
          </div>
        </div>
      </ModalHeader>
      <ModalContent>{passage}</ModalContent>
    </div>
  )
}
