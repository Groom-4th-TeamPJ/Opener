import { Timer } from 'lucide-react'

export default function NewQuestionHeader() {
  return (
    <div>
      <div className="flex justify-between">
        <h2 className="font-bold text-lg">변형 문제</h2>
        <div className="flex gap-1">
          <Timer width={24} height={24} className="stroke-Neutral-600" />
          <span>{/* 스탑워치 구현 예정*/}</span>
        </div>
      </div>
    </div>
  )
}
