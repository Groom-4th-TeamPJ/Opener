export interface CodeName {
  code: string
  name: string
}

export interface Passage {
  order: number
  type: 'text' | 'image'
  text: string | null
  url: string | null
}

export interface Option {
  order: number
  text: string
}

export type Level = 'EASY' | 'MEDIUM' | 'HARD'

export interface Question {
  questionId: number
  order: number
  category: CodeName
  level: Level
  point: number
  type: 'MCQ' | 'FRQ'
  passages: Passage[]
  options: Option[] | null
  answer: number
}

export interface Exam {
  examId: number
  year: number
  examType: CodeName
}

export interface ExamResponse {
  exam: Exam
  questions: Question[]
}
