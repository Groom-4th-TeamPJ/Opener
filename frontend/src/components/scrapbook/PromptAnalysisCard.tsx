import { Card, CardHeader, CardContent } from '@/components/common/Card'
import AISparklesIcon from '../icons/AISparklesIcon'

interface PromptAnalysisCardProps {
  content?: string
}

export default function PromptAnalysisCard({ content }: PromptAnalysisCardProps) {
  return (
    <Card className="flex flex-col flex-1 overflow-hidden scrollbar-overlay min-h-57 2xl:min-h-91.75">
      <CardHeader className="p-4 2xl:p-6" left={<AISparklesIcon className="w-6 h-6" />}>
        <h3 className="font-bold text-text-primary -ml-2">프롬프트 내용 요약</h3>
      </CardHeader>

      <CardContent className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col justify-start items-start p-0 px-4 2xl:px-6 wrap-break-word">
        {content && (
          <p className="text-sm 2xl:text-base text-text-primary leading-relaxed whitespace-pre-wrap">
            {content}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
