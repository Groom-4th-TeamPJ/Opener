import cn from '@/utils/cn'
import Input from '@/components/common/Input'
import { Card, CardContent } from '@/components/common/Card'
import type { Question } from '@/types/exam'
import { getChoiceStyle } from '@/utils/exam-styles'

interface AnswerCardProps {
  question: Question
  selectedChoice: number | null
  frqAnswer: string
  submitted: boolean
  isCorrect: boolean | null
  onChoiceSelect: (index: number) => void
  onFrqAnswerChange: (value: string) => void
}

export default function AnswerCard({
  question,
  selectedChoice,
  frqAnswer,
  submitted,
  isCorrect,
  onChoiceSelect,
  onFrqAnswerChange,
}: AnswerCardProps) {
  return (
    <Card className="overflow-hidden min-h-102.5">
      <CardContent className="px-6 pt-6 pb-2">
        <h3 className="text-text-secondary text-lg font-bold leading-7">답안 입력</h3>
      </CardContent>

      {/* Choices - Only for MCQ */}
      {question.type === 'MCQ' && question.options && (
        <CardContent className="p-6 flex flex-col gap-4">
          {question.options.map((option, index) => (
            <button
              key={option.order}
              onClick={() => onChoiceSelect(index)}
              disabled={submitted}
              className={cn(
                'h-12 px-4 bg-neutral-50 rounded-lg flex items-center gap-2 transition-all',
                getChoiceStyle({
                  index,
                  selectedChoice,
                  submitted,
                  correctAnswer: question.answer,
                  isCorrect,
                }),
                !submitted && 'cursor-pointer hover:bg-neutral-100'
              )}
            >
              <div
                className={cn(
                  'w-5 h-5 rounded-full border-[0.83px]',
                  selectedChoice === index
                    ? 'bg-primary-600 border-primary-600'
                    : 'bg-white border-neutral-600'
                )}
              />
              <div className="text-center text-text-primary leading-6">{option.text}</div>
            </button>
          ))}
        </CardContent>
      )}

      {/* FRQ Answer Input */}
      {question.type === 'FRQ' && (
        <CardContent className="p-6">
          <Input
            type="text"
            placeholder="답을 입력하세요"
            value={frqAnswer}
            onChange={(e) => onFrqAnswerChange(e.target.value)}
            disabled={submitted}
          />
        </CardContent>
      )}
    </Card>
  )
}
