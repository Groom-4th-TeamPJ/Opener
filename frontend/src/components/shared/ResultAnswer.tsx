import cn from '@/utils/cn'
import Input from '@/components/common/Input'
import { Check } from 'lucide-react'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import renderLatex from '@/utils/render-latex'

type ResultAnswer = {
  checked: boolean
  onChange: () => void
  disabled: boolean
  isWrong: boolean
  isCorrect: boolean
  name: string
  className?: string
  optionText: string
}
export default function ResultAnswer({
  checked,
  onChange,
  disabled,
  isWrong,
  isCorrect,
  name,
  className,
  optionText,
}: ResultAnswer) {
  return (
    <label
      className={cn(
        'group flex items-center gap-2',
        disabled ? 'cursor-default' : 'cursor-pointer',
        className
      )}
    >
      {/* 실제 라디오버튼 */}
      <Input
        type="radio"
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        name={name}
        className="sr-only absolute "
      />
      {/* 바깥 원 */}
      <span
        className={cn(
          'relative flex items-center justify-center w-4.5 h-4.5 lg:w-5 lg:h-5 rounded-full border bg-white border-gray-400 transition-colors ',
          checked && 'border-none bg-primary-600',
          isCorrect && 'border-success-600 bg-success-600',
          isWrong && 'border-danger-600 bg-danger-600'
        )}
      >
        {/* 내부 아이콘 */}
        <span className="absolute rounded-full bg-primary-600 scale-0 transition-transform " />
        {checked && !isCorrect && !isWrong && (
          <Check className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
        )}
        {isCorrect && (
          <CorrectAnswerIcon className="absolute size-4 text-white opacity-100 scale-75 transition-all " />
        )}
        {isWrong && (
          <WrongAnswerIcon className="absolute size-5 text-white opacity-100 scale-75 transition-all " />
        )}
      </span>

      {/* 보기 텍스트 */}
      <div dangerouslySetInnerHTML={{ __html: renderLatex(optionText) }} />
    </label>
  )
}
