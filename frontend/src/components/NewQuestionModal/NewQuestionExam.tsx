import { NewQuestionExamProps } from '@/types/exam-variant'

export default function NewQuestionExam({ passage }: NewQuestionExamProps) {
  return <div className="md:pb-8 lg:pb-16">{passage}</div>
}
