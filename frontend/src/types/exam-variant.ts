export type Option = {
  order: number
  text: string
}

<<<<<<< HEAD
export type ExamVariantValues = {
=======
export type NewQuestionModalValues = {
>>>>>>> frontend
  data: {
    questionNewId: number
    category: string
    type: string
    passage: string
    options: Option[]
    answer: number
<<<<<<< HEAD
    analysis: unknown
  }
}

export type ActionSectionProps = {
=======
    analysis: string
  }
}

export type NewQuestionActionProps = {
>>>>>>> frontend
  isSubmitted: boolean
  selected: number | null
  onSubmit: () => void
  onBack: () => void
}

<<<<<<< HEAD
export type AnalysisProps = {
  analysis: unknown
=======
export type NewQuestionAnalysisProps = {
  analysis: string
>>>>>>> frontend
  isSubmitted: boolean
  isCorrect: boolean | null
}

<<<<<<< HEAD
export type AnswerSectionProps = {
=======
export type NewQuestionAnswerProps = {
>>>>>>> frontend
  options: Option[]
  answer: number
  selected: number | null
  isSubmitted: boolean
  onSelect: (order: number) => void
}

<<<<<<< HEAD
export type QuestionSectionProps = {
  passage: string
  onClose: () => void
=======
export type NewQuestionExamProps = {
  passage: string
>>>>>>> frontend
}

export type ResultFeedbackProps = {
  isSubmitted: boolean
  isCorrect: boolean | null
}
<<<<<<< HEAD
=======

export type ResultBannerProps = {
  result: 'correct' | 'wrong'
}
>>>>>>> frontend
