export type Option = {
  order: number
  text: string
}

export type ExamVariantValues = {
  data: {
    questionNewId: number
    category: string
    type: string
    passage: string
    options: Option[]
    answer: number
    analysis: unknown
  }
}

export type ActionSectionProps = {
  isSubmitted: boolean
  selected: number | null
  onSubmit: () => void
  onBack: () => void
}

export type AnalysisProps = {
  analysis: unknown
  isSubmitted: boolean
  isCorrect: boolean | null
}

export type AnswerSectionProps = {
  options: Option[]
  answer: number
  selected: number | null
  isSubmitted: boolean
  onSelect: (order: number) => void
}

export type QuestionSectionProps = {
  passage: string
  onClose: () => void
}

export type ResultFeedbackProps = {
  isSubmitted: boolean
  isCorrect: boolean | null
}
