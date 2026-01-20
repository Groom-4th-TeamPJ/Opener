import { ApiSuccess } from './api.types'
import { CodeName } from './exam'

export interface ScrapbookData {
  examId: number
  examYear: number
  examType: CodeName
  openerUsageCount: number
  lastOpenerUsageDate: string
}

export type ScrapBookListData = {
  examYear: number
  examType: CodeName
  questionResults: {
    content: {
      questionResultId: number
      category: CodeName
      questionNo: number
      passage: string
      openerUsedAt: string
    }[]

    page: number
    size: number
    totalElements: number
    totalPages: number
    last: boolean
  }
}

export type ScrapBookListResponse = ApiSuccess<ScrapBookListData>
