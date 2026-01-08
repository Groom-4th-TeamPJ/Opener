export type Option = {
  order: number
  text: string
}

export type NewQuestion = {
  questionNewId: number
  category: string
  type: string
  passage: string
  options: Option[] | null
  answer: number
  analysis: string
}
export type NewQuestionModalValues = {
  data: NewQuestion[]
}

export type NewQuestionActionProps = {
  type: string
  isSubmitted: boolean
  selected: number | null
  frqAnswer: number | null
  onSubmit: () => void
  onBack: () => void
}

export type NewQuestionAnalysisProps = {
  analysis: string
  isSubmitted: boolean
  isCorrect: boolean | null
}

export type NewQuestionAnswerProps = {
  options: Option[]
  answer: number
  selected: number | null
  frqAnswer: number | null
  isSubmitted: boolean
  type: string //MCQ' | 'FRQ'
  onSelect: (order: number) => void
  onFrqChange: (answer: number) => void //단답식
}

export type NewQuestionExamProps = {
  passage: string
}

export type ResultFeedbackProps = {
  isSubmitted: boolean
  isCorrect: boolean | null
}

export type ResultBannerProps = {
  result: 'correct' | 'wrong'
}
