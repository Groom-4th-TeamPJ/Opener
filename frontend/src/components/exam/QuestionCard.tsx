import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import Input from '@/components/common/Input'
import { Card, CardContent } from '@/components/common/Card'
import type { Question } from '@/types/exam'
import { getChoiceStyle } from '@/utils/exam-styles'

interface QuestionCardProps {
  question: Question
  selectedChoice: number | null
  frqAnswer: string
  submitted: boolean
  isCorrect: boolean | null
  onChoiceSelect: (index: number) => void
  onFrqAnswerChange: (value: string) => void
}

export default function QuestionCard({
  question,
  selectedChoice,
  frqAnswer,
  submitted,
  isCorrect,
  onChoiceSelect,
  onFrqAnswerChange,
}: QuestionCardProps) {

  return (
    <Card className="overflow-hidden rounded-2xl">
        <CardContent className="p-8 min-h-50 flex flex-col justify-center border-b border-foreground/10">
          {question.passages.map((passage) => (
            <div key={passage.order} className="mb-2">
              {passage.type === 'text' ? (
                <p className="text-lg leading-relaxed">{passage.text}</p>
              ) : (
                <img src={passage.url || ''} alt="문제 이미지" className="w-full" />
              )}
            </div>
          ))}
        </CardContent>

        {/* Choices - Only for MCQ */}
        {question.type === 'MCQ' && question.options && (
          <CardContent className="p-4">
            <div className="space-y-2 max-w-md mx-auto">
              {question.options.map((option, index) => (
                <Button
                  key={option.order}
                  variant="ghost"
                  widthFull
                  onClick={() => onChoiceSelect(index)}
                  disabled={submitted}
                  className={cn(
                    'px-4 py-2.5 rounded-lg border-2 transition-all text-left text-sm font-medium justify-start',
                    getChoiceStyle({
                      index,
                      selectedChoice,
                      submitted,
                      correctAnswer: question.answer,
                      isCorrect,
                    }),
                    !submitted && 'cursor-pointer hover:shadow-md'
                  )}
                >
                  {option.order}. {option.text}
                </Button>
              ))}
            </div>

            {/* 정답/오답 결과 표시 */}
            {submitted && (
              <p
                className={cn(
                  'text-center mt-4 font-bold',
                  isCorrect ? 'text-green-600' : 'text-red-600'
                )}
              >
                {isCorrect ? '✓ 정답입니다!' : '✗ 오답입니다'}
              </p>
            )}
          </CardContent>
        )}

        {/* FRQ Answer Input */}
        {question.type === 'FRQ' && (
          <CardContent className="p-4">
            <Input
              type="text"
              placeholder="답을 입력하세요"
              value={frqAnswer}
              onChange={(e) => onFrqAnswerChange(e.target.value)}
              disabled={submitted}
            />

            {/* 정답/오답 결과 표시 */}
            {submitted && (
              <p
                className={cn(
                  'text-center mt-4 font-bold',
                  isCorrect ? 'text-green-600' : 'text-red-600'
                )}
              >
                {isCorrect ? '✓ 정답입니다!' : '✗ 오답입니다'}
              </p>
            )}
          </CardContent>
        )}
      </Card>
  )
}
