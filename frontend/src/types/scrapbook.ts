import { CodeName } from './exam'

export interface ScrapbookData {
  examResultId: number
  year: number
  examType: CodeName
  analysisCount: number
  recentDate: string
}
