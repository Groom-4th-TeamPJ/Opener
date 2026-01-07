import { Check } from 'lucide-react'
import cn from '@/utils/cn'
import type { NewQuestionAnswerProps } from '@/types/exam-variant'
import Input from '@/components/common/Input'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'

import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import FRQAnswer from '@/components/shared/FRQAnswer'

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
          <FRQAnswer variant="correct">
            <CorrectAnswerIcon />
            {answer}
          </FRQAnswer>
        )}
        {/* 오답 시 사용자가 입력한 답과 정답 */}
        {isSubmitted && isWrong && (
          <>
            <FRQAnswer variant="wrong">
              <WrongAnswerIcon />
              {frqAnswer}
            </FRQAnswer>

            <FRQAnswer variant="correct">
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
          <div
            key={option.order}
            className={cn(
              'flex items-center p-3  rounded-lg bg-neutral-50 md:h-10 lg:h-12',
              isCorrectChecked && 'bg-success-200',
              isWrongChecked && ' bg-danger-200'
            )}
          >
            <label
              className={cn(
                'group flex items-center gap-2',
                isSubmitted ? 'cursor-default' : 'cursor-pointer'
              )}
            >
              <div>
                {/* 실제 라디오 */}
                <Input
                  type="radio"
                  checked={isChecked}
                  onChange={() => onSelect(option.order)}
                  disabled={isSubmitted}
                  name="answer"
                  className="sr-only absolute"
                />
                {/* 바깥 원 */}
                <span
                  className={cn(
                    'relative flex items-center justify-center size-5 rounded-full border bg-white border-gray-400 transition-colors ',
                    !isSubmitted && isChecked && 'border-none bg-primary-600',
                    isCorrectChecked && 'border-success-600 bg-success-600',
                    isWrongChecked && 'border-danger-600 bg-danger-600'
                  )}
                >
                  {/* 안쪽 점 */}
                  <span className="absolute size-5 rounded-full bg-primary-600 scale-0 transition-transform " />
                  {!isSubmitted && isChecked && (
                    <Check className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
                  )}
                  {isCorrectChecked && (
                    <CorrectAnswerIcon className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
                  )}
                  {isWrongChecked && isChecked && (
                    <WrongAnswerIcon className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
                  )}
                </span>
              </div>

              {/* 텍스트 */}
              <div className="text-sm ">{option.text}</div>
            </label>
          </div>
        )
      })}
    </div>
  )
}
