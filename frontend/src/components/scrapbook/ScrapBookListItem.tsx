import { ROUTES } from '@/constants/routes'
import Link from 'next/link'
type ScrapListItemProps = {
  questionResultId: number
  categoryName: string
  passage: string
  createdAt: string
}
export default function ScrapBookListItem({
  questionResultId,
  categoryName,
  passage,
  createdAt,
}: ScrapListItemProps) {
  return (
    <Link href={`${ROUTES.SCRAPBOOK}/history/${questionResultId}`}>
      <div className="grid grid-cols-[1fr_4fr_1fr] gap-5 border-b border-b-neutral-200 px-3 h-15  items-center hover:bg-neutral-50">
        <span className="truncate">{categoryName}</span>
        <span className="truncate">{passage}</span>
        <span className="truncate">{createdAt}</span>
      </div>
    </Link>
  )
}
