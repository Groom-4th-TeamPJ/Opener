import cn from '@/utils/cn'
import Input from '@/components/common/Input'
import { Card, CardContent, CardHeader } from '@/components/common/Card'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import FRQAnswer from '@/components/shared/FRQAnswer'
import ResultAnswer from '@/components/shared/ResultAnswer'
import ResultBanner from '@/components/shared/ResultBanner'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'

export default function AnswerCard() {
  const examData = useCurrentExam()
  const { currentIndex, getQuestionState, updateQuestionState } = useExamStore()

  if (!examData) return null

  const { questions } = examData
  const question = questions[currentIndex]
  const { selectedChoice, frqAnswer, isSubmitted, isCorrect, correctAnswer } = getQuestionState(
    question.questionId
  )

  const handleChoiceSelect = (index: number) => {
    if (isSubmitted || question.questionType === 'FRQ') return
    updateQuestionState(question.questionId, { selectedChoice: index })
  }

  const handleFrqAnswerChange = (value: string) => {
    updateQuestionState(question.questionId, { frqAnswer: value })
  }

  return (
    <Card className="overflow-hidden min-h-81">
      <CardHeader>
        <div className="flex items-center justify-between h-8">
          <h3 className="text-text-secondary lg:text-lg font-bold">답안 입력</h3>

          {isSubmitted && isCorrect !== null && (
            <ResultBanner result={isCorrect ? 'correct' : 'wrong'} />
          )}
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-3 lg:gap-4 -mt-6">
        {/* Choices - Only for MCQ */}
        {question.questionType === 'MCQ' && question.options && (
          <>
            {question.options.map((option) => {
              const isChecked = selectedChoice === option.order
              const isAnswer = option.order === correctAnswer
              const isCorrectChecked = isSubmitted && isAnswer
              const isWrongChecked = isSubmitted && isChecked && !isAnswer

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
                    onChange={() => handleChoiceSelect(option.order)}
                    disabled={isSubmitted}
                    isWrong={isWrongChecked}
                    isCorrect={isCorrectChecked}
                    name="answer"
                    optionText={option.content}
                  />
                </FRQAnswer>
              )
            })}
          </>
        )}

        {/* FRQ Answer Input */}
        {question.questionType === 'FRQ' && (
          <>
            {!isSubmitted && (
              <Input
                type="text"
                value={frqAnswer}
                placeholder="생각한 답안을 입력해주세요"
                disabled={isSubmitted}
                onChange={(e) => {
                  const answer = e.target.value
                  // 숫자만 허용
                  if (answer === '' || /^\d+$/.test(answer)) {
                    handleFrqAnswerChange(answer)
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
            {isSubmitted && isCorrect && (
              <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
                <CorrectAnswerIcon />
                {correctAnswer}
              </FRQAnswer>
            )}
            {/* 오답 시 사용자가 입력한 답과 정답 */}
            {isSubmitted && !isCorrect && (
              <>
                <FRQAnswer variant="wrong" className="md:h-12 lg:h-15">
                  <WrongAnswerIcon />
                  {frqAnswer}
                </FRQAnswer>

                <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
                  <CorrectAnswerIcon />
                  {correctAnswer}
                </FRQAnswer>
              </>
            )}
            {!isSubmitted && <div className="text-xs text-neutral-600">숫자만 입력</div>}
          </>
        )}
      </CardContent>
    </Card>
  )
}
