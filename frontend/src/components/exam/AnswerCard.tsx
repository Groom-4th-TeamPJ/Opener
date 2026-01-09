import cn from '@/utils/cn'
import Input from '@/components/common/Input'
import { Card, CardContent, CardHeader } from '@/components/common/Card'
import type { Question } from '@/types/exam'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import FRQAnswer from '@/components/shared/FRQAnswer'
import ResultAnswer from '@/components/shared/ResultAnswer'

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
    <Card className="overflow-hidden min-h-81">
      <CardHeader>
        <h3 className="text-text-secondary lg:text-lg font-bold">답안 입력</h3>
      </CardHeader>

      <CardContent className="flex flex-col gap-3 lg:gap-4 -mt-6">
        {/* Choices - Only for MCQ */}
        {question.type === 'MCQ' && question.options && (
          <>
            {question.options.map((option) => {
              const isChecked = selectedChoice === option.order
              const isAnswer = option.order === question.answer
              const isCorrectChecked = submitted && isAnswer
              const isWrongChecked = submitted && isChecked && !isAnswer

              return (
                <FRQAnswer
                  variant="default"
                  key={option.order}
                  className={cn(
                    'flex items-center p-3 rounded-lg lg:h-12',
                    isCorrectChecked && 'bg-success-200',
                    isWrongChecked && 'bg-danger-200'
                  )}
                >
                  <ResultAnswer
                    checked={isChecked}
                    onChange={() => onChoiceSelect(option.order)}
                    disabled={submitted}
                    isWrong={isWrongChecked}
                    isCorrect={isCorrectChecked}
                    name="answer"
                    optionText={option.text}
                  />
                </FRQAnswer>
              )
            })}
          </>
        )}

        {/* FRQ Answer Input */}
        {question.type === 'FRQ' && (
          <>
            {!submitted && (
              <Input
                type="text"
                value={frqAnswer}
                placeholder="생각한 답안을 입력해주세요"
                disabled={submitted}
                onChange={(e) => {
                  const answer = e.target.value
                  // 숫자만 허용
                  if (answer === '' || /^\d+$/.test(answer)) {
                    onFrqAnswerChange(answer)
                  }
                }}
                className={cn(
                  'lg:h-15 lg:px-6 lg:text-xl lg:placeholder:text-lg',
                  'placeholder:text-neutral-200',
                  'disabled:bg-white disabled:text-neutral-900'
                )}
              />
            )}
            {/* 정답 시 사용자가 입력한 답안 */}
            {submitted && isCorrect && (
              <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
                <CorrectAnswerIcon />
                {question.answer}
              </FRQAnswer>
            )}
            {/* 오답 시 사용자가 입력한 답과 정답 */}
            {submitted && !isCorrect && (
              <>
                <FRQAnswer variant="wrong" className="md:h-12 lg:h-15">
                  <WrongAnswerIcon />
                  {frqAnswer}
                </FRQAnswer>

                <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
                  <CorrectAnswerIcon />
                  {question.answer}
                </FRQAnswer>
              </>
            )}
            {!submitted && <div className="text-xs text-neutral-600">숫자만 입력</div>}
          </>
        )}
      </CardContent>
    </Card>
  )
}
