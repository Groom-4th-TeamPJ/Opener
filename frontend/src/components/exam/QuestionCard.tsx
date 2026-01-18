import Image from 'next/image'
import { Card, CardHeader, CardContent } from '@/components/common/Card'
import type { StopwatchRef } from '@/types/exam'
import QuestionTitle from './QuestionTitle'
import renderLatex from '@/utils/render-latex'
import useCurrentExam from '@/hooks/exam/use-current-exam'
import { useExamStore } from '@/stores/use-exam-store'

interface QuestionCardProps {
  stopwatchRef?: React.Ref<StopwatchRef>
}

export default function QuestionCard({ stopwatchRef }: QuestionCardProps) {
  const examData = useCurrentExam()
  const { currentIndex } = useExamStore()

  if (!examData) return null

  const { questions } = examData
  const question = questions[currentIndex]

  return (
    <Card className="flex flex-col flex-1 overflow-hidden min-h-70">
      <CardHeader className="p-6 shrink-0">
        <QuestionTitle question={question} stopwatchRef={stopwatchRef} />
      </CardHeader>

      <CardContent className="flex-1 min-h-0  flex flex-col pt-0 px-6">
        <div className="flex-1 min-h-0 overflow-y-auto scrollbar-overlay flex flex-col justify-start items-start gap-2.5">
          {question.passages.map((passage) => (
            <div
              key={passage.order}
              className="self-stretch flex flex-col justify-start items-start gap-1"
            >
              {passage.type === 'TEXT' && (
                <div
                  className="leading-7"
                  dangerouslySetInnerHTML={{ __html: renderLatex(passage.content || '') }}
                />
              )}
              {passage.type === 'IMAGE' && passage.content && (
                <Image
                  src={passage.content}
                  alt={`문제 이미지 ${passage.order}`}
                  width={400}
                  height={400}
                  className="max-w-full h-auto"
                />
              )}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
