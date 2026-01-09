import { Card, CardHeader, CardContent, CardFooter } from '@/components/common/Card'
import ResultBanner from '@/components/shared/ResultBanner'
import type { Exam, Question, StopwatchRef } from '@/types/exam'
import QuestionTitle from './QuestionTitle'

interface QuestionCardProps {
  exam: Exam
  question: Question
  stopwatchRef?: React.Ref<StopwatchRef>
  submitted: boolean
  isCorrect: boolean | null
}

export default function QuestionCard({
  exam,
  question,
  stopwatchRef,
  submitted,
  isCorrect,
}: QuestionCardProps) {
  return (
    <Card className="flex flex-col flex-1 overflow-hidden min-h-117.5">
      <CardHeader className="p-6">
        <QuestionTitle exam={exam} question={question} stopwatchRef={stopwatchRef} />
      </CardHeader>

      <CardContent className="flex-1 px-6 py-3 flex flex-col justify-start items-start gap-2.5">
        {question.passages.map((passage) => (
          <div
            key={passage.order}
            className="self-stretch flex flex-col justify-start items-start gap-1"
          >
            <p className="leading-6">{passage.text}</p>
          </div>
        ))}
      </CardContent>

      <CardFooter>
        {submitted && isCorrect !== null && (
          <ResultBanner result={isCorrect ? 'correct' : 'wrong'} />
        )}
      </CardFooter>
    </Card>
  )
}
