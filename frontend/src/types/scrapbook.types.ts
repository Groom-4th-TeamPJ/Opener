import { CodeName } from './exam'

export interface ScrapbookData {
  examId: number
  examYear: number
  examType: CodeName
  openerUsageCount: number
  lastOpenerUsageDate: string
}
