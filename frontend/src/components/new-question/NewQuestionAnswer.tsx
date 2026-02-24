import cn from '@/utils/cn'
import type { NewQuestionAnswerProps } from '@/types/exam-variant'
import Input from '@/components/common/Input'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'

import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import FRQAnswer from '@/components/shared/FRQAnswer'
import ResultAnswer from '../shared/ResultAnswer'

export default function NewQuestionAnswer({
  type,
  options,
  answer,
  selected,
  isSubmitted,
  onSelect,
  onFrqChange,
  frqAnswer,
}: NewQuestionAnswerProps) {
  if (type === 'FRQ') {
    const isCorrect = isSubmitted && frqAnswer === answer
    const isWrong = isSubmitted && frqAnswer !== answer

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const answer = e.target.value
      // 숫자만 허용
      if (!/^\d+$/.test(answer)) return
      onFrqChange(Number(answer))
    }
    return (
      <div className="flex flex-col gap-2">
        {!isSubmitted && (
          <Input
            type="text"
            value={frqAnswer ?? ''}
            placeholder="생각한 답안을 입력해주세요"
            disabled={isSubmitted}
            onChange={handleChange}
            className={cn(
              'lg:h-16 lg:px-6 lg:text-xl lg:placeholder:text-lg',
              isCorrect && ' border-success-600!',
              isWrong && ' border-danger-600!',
              'placeholder:text-neutral-200',
              'disabled:bg-white disabled:text-neutral-900 '
            )}
          />
        )}
        {/* 정답 시 사용자가 입력한 답안 */}
        {isSubmitted && isCorrect && (
          <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
            <CorrectAnswerIcon />
            {answer}
          </FRQAnswer>
        )}
        {/* 오답 시 사용자가 입력한 답과 정답 */}
        {isSubmitted && isWrong && (
          <>
            <FRQAnswer variant="wrong" className="md:h-12 lg:h-15">
              <WrongAnswerIcon />
              {frqAnswer}
            </FRQAnswer>

            <FRQAnswer variant="correct" className="md:h-12 lg:h-15">
              <CorrectAnswerIcon />
              {answer}
            </FRQAnswer>
          </>
        )}
        {!isSubmitted && <div className="text-xs text-neutral-600">숫자만 입력</div>}
      </div>
    )
  }
  return (
    <div className="flex flex-col gap-2">
      {options.map((option) => {
        //답안 선택
        const isChecked = selected === option.order
        //실제 정답
        const isAnswer = option.order === answer
        // 제출 후 보여질 정답
        const isCorrectChecked = isSubmitted && isAnswer
        //제출 후 보여질 내 오답
        const isWrongChecked = isSubmitted && isChecked && !isAnswer

        // 제출 후 오답일 때 정답, 선택한 오답만 보여주기
        if (isSubmitted && selected !== answer && !isChecked && !isAnswer) {
          return null
        }

        return (
          <FRQAnswer
            variant="default"
            key={option.order}
            className={cn(
              'flex items-center p-3 rounded-lg lg:h-12',
              isCorrectChecked && 'bg-success-200',
              isWrongChecked && ' bg-danger-200'
            )}
          >
            <ResultAnswer
              checked={isChecked}
              onChange={() => onSelect(option.order)}
              disabled={isSubmitted}
              isWrong={isWrongChecked}
              isCorrect={isCorrectChecked}
              name={'answer'}
              optionText={option.content}
            />
          </FRQAnswer>
        )
      })}
    </div>
  )
}
