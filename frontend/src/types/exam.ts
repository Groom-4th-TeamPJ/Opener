export interface CodeName {
  code: string
  name: string
}

export interface Passage {
  order: number
  type: 'TEXT' | 'IMAGE'
  text: string | null
  url: string | null
}

export interface Option {
  order: number
  text: string
}

export interface Question {
  questionId: number
  questionNo: number
  category?: CodeName
  point: number
  type: 'MCQ' | 'FRQ'
  passages: Passage[]
  options: Option[] | null
  answer: number
}

export interface Exam {
  examId: number
  examYear: number
  examType: CodeName
}

export interface ExamResponse {
  exam: Exam
  questions: Question[]
}

export interface ChatMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  timestamp: string
  highlight?: string
}

export interface ScrapbookChatMessage {
  order: number
  role: 'USER' | 'ASSISTANT'
  content: string
  timestamp: string
}

export interface ScrapbookQuestion extends Question {
  examYear: number
  examType: CodeName
  createdAt: string
  select: number
  chat: ScrapbookChatMessage[]
}

export type ResultData = {
  solveTime: string
  correctCount: number
  wrongCount: number
  openerCount: number
}

export interface StopwatchRef {
  start: () => void
  stop: () => void
  reset: () => void
  getTime: () => number
}

export interface StopwatchProps {
  onTimeChange?: (seconds: number) => void
  autoStart?: boolean
  ref?: React.Ref<StopwatchRef>
}
