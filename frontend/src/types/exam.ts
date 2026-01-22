export interface CodeName {
  code: string
  name: string
}

export interface Passage {
  order: number
  type: 'TEXT' | 'IMAGE'
  content: string
  url?: string
}

export interface Option {
  order: number
  content: string
}

export interface Question {
  questionId: number
  questionNo: number
  category?: CodeName
  point: number
  questionType: 'MCQ' | 'FRQ'
  passages: Passage[]
  options: Option[] | null
  answer: number
}

export interface Exam {
  examId: number
  examYear: number
  examType: CodeName
  name: string
  quantity: number
}

// 시험을 식별하기 위한 공통 파라미터 타입
export interface ExamRequestParams {
  examYear: number
  category: string
  examType: string
}

export interface ExamResponse {
  examResultId: number
  exam: Exam
  questions: Question[]
}

export interface SubmitAnswerRequest {
  selected: number
  timeSpent: number
}

export interface SubmitAnswerResponse {
  questionResultId: number
  correct: boolean
  answer: number
}

export interface ChatMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  timestamp: string
  isStreaming?: boolean
}

export interface SendChatMessageRequest {
  sessionId: number
  questionId: number
  message: string
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
  selected: number
  chat: ScrapbookChatMessage[]
  promptSummary?: string
}

export type ResultData = {
  totalTimeSpent: number
  correctCount: number
  incorrectCount: number
  openerUsageCount: number
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
