import { CodeName } from './exam'

export interface ScrapbookData {
  examResultId: number
  examYear: number
  examType: CodeName
  analysisCount: number
  recentDate: string
}
