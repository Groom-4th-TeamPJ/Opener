import { NewQuestionExamProps } from '@/types/exam-variant'
import renderLatex from '@/utils/render-latex'

export default function NewQuestionExam({ passage }: NewQuestionExamProps) {
  return (
    <div
      className="md:pb-8 lg:pb-16 leading-7"
      dangerouslySetInnerHTML={{ __html: renderLatex(passage) }}
    ></div>
  )
}
