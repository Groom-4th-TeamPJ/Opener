import { Check, CircleStop, X } from 'lucide-react'
import cn from '@/utils/cn'
import type { NewQuestionAnswerProps } from '@/types/exam-variant'
import Input from '@/components/common/Input'
import CorrectAnswerIcon from '../icons/CorrectAnswerIcon'

import WrongAnswerIcon from '../icons/WrongAnswerIcon'

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

    return (
      <div className="flex flex-col gap-2">
        <div className="relative">
          <Input
            type="text"
            value={frqAnswer || ''}
            placeholder="생각한 답안을 적어주세요"
            disabled={isSubmitted}
            onChange={(e) => onFrqChange(e.target.value === '' ? null : Number(e.target.value))}
            className={cn(
              'pl-9.5 md:h-12! lg:h-15!',

              isCorrect && ' border-success-600!',
              isWrong && ' border-danger-600!',
              'disabled:bg-white disabled:text-neutral-900 '
            )}
          />
          {isSubmitted && (
            <div className={cn('absolute left-3 ', 'top-1/2 -translate-y-1/2')}>
              {isCorrect ? <CorrectAnswerIcon /> : <WrongAnswerIcon />}
            </div>
          )}
        </div>
        {isSubmitted && isWrong && (
          <div
            className={cn(
              'flex items-center p-3 border rounded-lg border-success-600 mt-4 gap-1.5 md:h-12 lg:h-15'
            )}
          >
            <CorrectAnswerIcon />
            {answer}
          </div>
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

        return (
          <div
            key={option.order}
            className={cn(
              'flex items-center p-3 border rounded-lg border-neutral-200 md:h-10 lg:h-12',
              isSubmitted && isChecked && 'border-primary-600',
              isCorrectChecked && 'border-success-600',
              isWrongChecked && 'border border-danger-600'
            )}
          >
            <label className="group flex items-center cursor-pointer gap-2">
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
                    'relative flex items-center justify-center size-5 rounded-full border border-gray-400 transition-colors ',
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
                  {/* 임시 아이콘 */}
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
