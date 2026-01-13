import { Card, CardContent, CardFooter, CardHeader } from '@/components/common/Card'
import { Badge } from '@/components/common/Badge'
import { ScrapbookData } from '@/types/scrapbook'
import Link from 'next/link'
import { ROUTES } from '@/constants/routes'

const EXAM_TYPES: Record<string, string> = {
  M06: '6월 모의평가',
  M09: '9월 모의평가',
  CSAT: '수학능력시험',
}

export default function AnalysisCard({
  userResultId,
  year,
  examType,
  analysisCount,
  recentDate,
}: ScrapbookData) {
  return (
    <Link href={`${ROUTES.SCRAPBOOK}/${userResultId}`}>
      <Card className="hover:shadow-2">
        <CardHeader className="py-4">
          <Badge label={year.toString()} variant="secondary" type="solid-pastel" />
        </CardHeader>
        <CardContent className="pt-0 pb-4 space-y-2">
          <h1 className="lg:text-lg font-bold">{EXAM_TYPES[examType.code]}</h1>
          <h2 className="text-sm">
            오프너 분석 <span className="text-primary-600">{analysisCount}개</span>
          </h2>
        </CardContent>
        <CardFooter className="py-4 justify-between border-t border-neutral-100 text-text-secondary text-sm">
          <span>최근 분석 날짜</span> <span>{recentDate}</span>
        </CardFooter>
      </Card>
    </Link>
  )
}
