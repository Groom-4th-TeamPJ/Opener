export type Option = {
  order: number
  text: string
}

export type NewQuestionModalValues = {
  data: {
    questionNewId: number
    category: string
    type: string
    passage: string
    options: Option[]
    answer: number
    analysis: string
  }
}

export type NewQuestionActionProps = {
  isSubmitted: boolean
  selected: number | null
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
  isSubmitted: boolean
  onSelect: (order: number) => void
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
