import { ApiSuccess } from './api.types'
import { CodeName } from './exam'

export interface ScrapbookResponse {
  data: ScrapbookData[]
}

export interface ScrapbookData {
  examResultId: number
  examYear: number
  examType: CodeName
  openerUsageCount: number
  lastOpenerUsageDate: string
}

export type ScrapBookListData = {
  examYear: number
  examType: { code: 'M06' | 'M09' | 'CSAT'; name: '6모' | '9모' | '수능' }
  questionResults: {
    questionResultId: number
    category: {
      code: 'ALG' | 'GEO' | 'PROB' | 'CALC'
      name: '수학I + 수학II' | '기하' | '확률' | '미적분'
    }
    questionNo: number
    passage: string
    createdAt: string
  }[]
}

export type ScrapBookListResponse = ApiSuccess<ScrapBookListData>
