import Image from 'next/image'
import Link from 'next/link'

type ScrapBookPaginationProps = {
  currentPage: number
  previousPage: number
  totalPages: number
  nextPage: number
}

export default function ScrapBookPagination({
  currentPage,
  previousPage,
  totalPages,
  nextPage,
}: ScrapBookPaginationProps) {
  const singlePage = totalPages <= 1

  return (
    <nav className="flex mt-4 gap-4 mx-auto" aria-label='스크랩북 목록 페이지네이션'>
      <ul className="flex gap-2">
        {!singlePage && currentPage > 1 && (
          <li>
            <Link href={`?page=${previousPage}`} aria-label={'이전 페이지'}>
              <Image
                src={'/icons/chevron-left.svg'}
                alt={'이전 페이지'}
                width={24}
                height={24}
              />
            </Link>
          </li>
        )}
        {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
          <li
            key={page}
            className={
              currentPage === page ? 'font-bold text-neutral-900' : 'font-medium text-neutral-600'
            }
          >
            <Link href={`?page=${page}`} aria-current={currentPage === page ? 'page': undefined} aria-label={`페이지 ${page}${currentPage === page ? ' (현재 페이지)' : ''}`} className='px-2 py-1'>{page}</Link>
          </li>
        ))}

        {!singlePage && currentPage < totalPages && (
          <li>
            <Link href={`?page=${nextPage}`} aria-label={'다음 페이지'}>
              <Image
                src={'/icons/chevron-right.svg'}
                alt={'다음 페이지'}
                width={24}
                height={24}
              />
            </Link>
          </li>
        )}
      </ul>
    </nav>
  )
}
