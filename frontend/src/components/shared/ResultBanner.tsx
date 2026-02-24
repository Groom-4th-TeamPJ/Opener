import type { ResultBannerProps } from '@/types/exam-variant'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'
import { Badge } from '@/components/common/Badge'
import cn from '@/utils/cn'

const RESULT_CONFIG = {
  correct: {
    textClass: 'text-success-600 font-bold',
    bgClass: 'bg-success-200',
    leftIcon: <CorrectAnswerIcon />,
    label: '정답입니다',
  },
  wrong: {
    textClass: 'text-danger-600 font-bold',
    bgClass: 'bg-danger-200',
    leftIcon: <WrongAnswerIcon />,
    label: '오답입니다',
  },
}
export default function ResultBanner({ result }: ResultBannerProps) {
  const { textClass, bgClass, leftIcon, label } = RESULT_CONFIG[result]

  return (
    <Badge
      size="md"
      variant="success"
      pill
      label={label}
      leftIcon={leftIcon}
      className={cn('h-8', bgClass, textClass)}
    />
  )
}
