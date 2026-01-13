import { CodeName } from './exam'

export interface ScrapbookData {
  userResultId: number
  year: number
  examType: CodeName
  analysisCount: number
  recentDate: string
}
