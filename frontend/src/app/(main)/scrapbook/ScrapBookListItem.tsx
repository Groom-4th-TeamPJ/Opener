import Link from 'next/link'
type ScrapListItemProps = {
  id: number
  categoryName: string
  passage: string
  createdAt: string
}
export default function ScrapRow({ id, categoryName, passage, createdAt }: ScrapListItemProps) {
  return (
    <Link href={`/scrapbook/${id}`}>
      <div className="grid grid-cols-[1fr_3fr_1fr] gap-5 border-b border-b-neutral-200 px-3 h-14 lg:h-18 items-center hover:bg-neutral-50">
        <span className="truncate">{categoryName}</span>
        <span className="truncate">{passage}</span>
        <span className="truncate">{createdAt}</span>
      </div>
    </Link>
  )
}
