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
  return (
    <nav className="flex mt-4 gap-4 mx-auto">
      <ul className="flex gap-2">
        {currentPage === 1 ? (
          <li>
            {/* 첫 번째 페이지에 위치한 경우 '이전 페이지 버튼' 비활성화 */}
            <span>
              <Image
                src={'/icons/chevron-left_gray.svg'}
                aria-label={'이전 페이지'}
                alt={'이전 페이지'}
                width={24}
                height={24}
              />
            </span>
          </li>
        ) : (
          <li>
            <Link href={`?page=${previousPage - 1}`}>
              <Image
                src={'/icons/chevron-left.svg'}
                aria-label={'이전 페이지'}
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
            <Link href={`?page=${page - 1}`}>{page}</Link>
          </li>
        ))}

        {currentPage === totalPages ? (
          <li>
            {/* 마지막 페이지에 위치한 경우 '다음 페이지 버튼' 비활성화 */}
            <span>
              <Image
                src={'/icons/chevron-right_gray.svg'}
                aria-label={'다음 페이지'}
                alt={'다음 페이지'}
                width={24}
                height={24}
              />
            </span>
          </li>
        ) : (
          <li>
            <Link href={`?page=${nextPage - 1}`}>
              <Image
                src={'/icons/chevron-right.svg'}
                aria-label={'다음 페이지'}
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
