import { ApiSuccess } from './api.types'

export type ScrapBookListData = {
  year: number
  examType: { code: 'M06' | 'M09' | 'CSAT'; name: '6모' | '9모' | '수능' }
  examHistories: {
    examHistoryId: number
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
