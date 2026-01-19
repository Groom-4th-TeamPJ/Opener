import { ApiSuccess } from './api.types'

export type ScrapBookListData = {
  examYear: number
  examType: { code: 'M06' | 'M09' | 'CSAT'; name: '6모' | '9모' | '수능' }
  questionResults: {
    content: {
      questionResultId: number
      category: {
        code: 'ALG' | 'GEO' | 'PROB' | 'CALC'
        name: '수학I + 수학II' | '기하' | '확률' | '미적분'
      }
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
