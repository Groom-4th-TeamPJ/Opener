import { ReadonlyURLSearchParams } from 'next/navigation'

type useScrapBookPaginationProps = {
  totalCount: number
  pageSize: number
  search: ReadonlyURLSearchParams
}
export default function useScrapBookPagination({
  totalCount,
  pageSize,
  search,
}: useScrapBookPaginationProps) {
  const currentPage = Math.max(1, Number(search.get('page') || '1'))
  const totalPages = Math.ceil(totalCount / pageSize)

  const previousPage = Math.max(1, currentPage - 1)
  const nextPage = Math.max(1, currentPage + 1)

  // 리스트 개수 8개 초과 시 페이지 추가
  const startIndex = (currentPage - 1) * pageSize
  const endIndex = startIndex + pageSize

  const pages = []
  for (let i = 1; i <= totalPages; i++) {
    pages.push(i)
  }

  return { currentPage, previousPage, nextPage, totalPages, startIndex, endIndex, pages }
}
