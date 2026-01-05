import { Check, CircleStop, X } from 'lucide-react'
import cn from '@/utils/cn'
import type { NewQuestionAnswerProps } from '@/types/exam-variant'
import Input from '@/components/common/Input'

export default function NewQuestionAnswer({
  options,
  answer,
  selected,
  isSubmitted,
  onSelect,
}: NewQuestionAnswerProps) {
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
                    <CircleStop className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
                  )}
                  {isWrongChecked && isChecked && (
                    <X className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
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
