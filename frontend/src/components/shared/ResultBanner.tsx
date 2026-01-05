import type { ResultBannerProps } from '@/types/exam-variant'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'

const RESULT_CONFIG = {
  correct: {
    textClass: 'text-success-600 font-bold',
    bgClass: 'bg-success-200',
    icon: <CorrectAnswerIcon />,
    label: '정답입니다.',
  },
  wrong: {
    textClass: 'text-danger-600 font-bold',
    bgClass: 'bg-danger-200',
    icon: <WrongAnswerIcon />,
    label: '오답입니다.',
  },
}
export default function ResultBanner({ result }: ResultBannerProps) {
  const { textClass, bgClass, icon, label } = RESULT_CONFIG[result]

  return (
    <div
      className={`p-4 flex justify-center items-center flex-1 gap-1 rounded-lg md:min-h-15 lg:min-h-20 ${bgClass}`}
    >
      {icon}
      <div className={textClass}>{label}</div>
    </div>
  )
}
