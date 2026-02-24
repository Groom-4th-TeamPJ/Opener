import { ROUTES } from '@/constants/routes'
import renderLatex from '@/utils/render-latex'

import Link from 'next/link'
type ScrapListItemProps = {
  questionResultId: number
  categoryName: string
  passage: string
  openerUsedAt: string
}
const DATE_FORMAT_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
}

export default function ScrapBookListItem({
  questionResultId,
  categoryName,
  passage,
  openerUsedAt,
}: ScrapListItemProps) {
  const formattedOpenerUsedAt = new Intl.DateTimeFormat('default', DATE_FORMAT_OPTIONS).format(
    new Date(openerUsedAt)
  )

  return (
    <Link href={`${ROUTES.SCRAPBOOK}/history/${questionResultId}`}>
      <div className="grid grid-cols-[1fr_4fr_1fr] gap-5 border-b border-b-neutral-200 px-3 h-15  items-center hover:bg-neutral-50">
        <span className="truncate">{categoryName}</span>

        <div className="min-w-0 overflow-hidden">
          <div
            className=" line-clamp-1"
            dangerouslySetInnerHTML={{ __html: renderLatex(passage, { blockDisplayMode: false }) }}
          />
        </div>
        <span className="truncate">{formattedOpenerUsedAt}</span>
      </div>
    </Link>
  )
}
