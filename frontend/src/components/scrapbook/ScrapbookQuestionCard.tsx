import Image from 'next/image'
import { Card, CardHeader, CardContent } from '@/components/common/Card'
import ScrapbookQuestionTitle from './ScrapbookQuestionTitle'
import type { ScrapbookQuestion } from '@/types/exam'
import cn from '@/utils/cn'
import CorrectAnswerIcon from '@/components/icons/CorrectAnswerIcon'
import WrongAnswerIcon from '@/components/icons/WrongAnswerIcon'
import FRQAnswer from '@/components/shared/FRQAnswer'
import ResultAnswer from '@/components/shared/ResultAnswer'

interface ScrapbookQuestionCardProps {
  data: ScrapbookQuestion
}

export default function ScrapbookQuestionCard({ data }: ScrapbookQuestionCardProps) {
  const isCorrect = data.select === data.answer

  return (
    <Card className="flex flex-col h-full overflow-hidden">
      <CardHeader className="p-4 2xl:p-6">
        <ScrapbookQuestionTitle question={data} />
      </CardHeader>

      <CardContent className="flex-1 overflow-y-auto scrollbar-overlay px-6 py-3 flex flex-col justify-start items-start gap-2.5">
        {/* 임시 이미지 추후 변경 */}
        {data.passages.map((passage) => (
          <div key={passage.order} className="flex flex-col justify-start items-start">
            {passage.type === 'TEXT' && <p className="leading-6">{passage.content}</p>}
            {passage.type === 'IMAGE' && passage.url && (
              <Image
                src={passage.url}
                alt="문제 영역 이미지"
                width={400}
                height={400}
                unoptimized
              />
            )}
          </div>
        ))}
      </CardContent>

      {/* 답안 영역 */}
      <div className="w-full p-4 2xl:p-6 border-t border-neutral-200">
        <div className="flex flex-col gap-3">
          <p className="text-sm 2xl:text-base font-bold text-text-secondary">답안 영역</p>

          {data.questionType === 'MCQ' && data.options ? (
            // 객관식
            <>
              {data.options.map((option) => {
                const isAnswer = option.order === data.answer
                const isSelected = option.order === data.select
                const isCorrectSelected = isSelected && isAnswer
                const isWrongSelected = isSelected && !isAnswer

                // 오답일 때는 정답과 선택한 답만 표시
                if (!isCorrect && !isSelected && !isAnswer) {
                  return null
                }

                return (
                  <FRQAnswer
                    variant="default"
                    key={option.order}
                    className={cn(
                      'flex items-center p-3 rounded-lg 2xl:h-12',
                      isCorrectSelected && 'bg-success-200',
                      isWrongSelected && 'bg-danger-200',
                      isAnswer && !isSelected && 'bg-success-200'
                    )}
                  >
                    <ResultAnswer
                      checked={isSelected}
                      onChange={() => {}}
                      disabled={true}
                      isWrong={isWrongSelected}
                      isCorrect={isAnswer}
                      name="answer"
                      optionText={option.content}
                    />
                  </FRQAnswer>
                )
              })}
            </>
          ) : (
            // 주관식
            <>
              {!isCorrect && (
                <FRQAnswer variant="wrong" className="h-12 2xl:h-15">
                  <WrongAnswerIcon />
                  {data.select}
                </FRQAnswer>
              )}
              <FRQAnswer variant="correct" className="h-12 2xl:h-15">
                <CorrectAnswerIcon />
                {data.answer}
              </FRQAnswer>
            </>
          )}
        </div>
      </div>
    </Card>
  )
}
