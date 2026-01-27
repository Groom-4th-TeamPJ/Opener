import Image from 'next/image'
import type { ScrapbookQuestion } from '@/types/exam'
import { Badge } from '@/components/common/Badge'
import { formatDate } from '@/utils/format'
import getPointVariant from '@/utils/get-point-variant'

interface ScrapbookQuestionTitleProps {
  question: ScrapbookQuestion
}

export default function ScrapbookQuestionTitle({ question }: ScrapbookQuestionTitleProps) {
  const pointVariant = getPointVariant(question.point)

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-6">
      <span className="bg-primary-600 text-white text-lg font-bold px-4 py-2 rounded-lg">
        {question.questionNo}
      </span>
      <div className="flex flex-col justify-center gap-0.5">
        <p className="text-lg 2xl:text-[1.375rem] text-text-primary font-bold leading-tight">
          {question.examYear}학년도 {question.examType.name}
        </p>
        <p className="text-sm text-text-secondary leading-tight">{question.questionNo}번 문항</p>
      </div>

      <div className="w-full xl:w-auto xl:ml-auto flex items-center gap-4">
        <Badge
          type="solid-pastel"
          size="md"
          variant={pointVariant}
          pill
          label={`${question.point}점`}
          className="rounded-sm xl:h-7 xl:px-4 lg:text-base"
        />
        <div className="flex items-center gap-2 text-text-primary">
          <Image src="/icons/calender_gray.svg" alt="캘린더 아이콘" width={24} height={24} />
          <span>{formatDate(question.createdAt)}</span>
        </div>
      </div>
    </div>
  )
}
