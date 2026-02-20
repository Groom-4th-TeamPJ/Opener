import ScrapBookPagination from "@/components/scrapbook/ScrapBookPagination";
import { describe, expect, test } from "vitest";
import { render, screen } from '@testing-library/react'

describe('ScrapBookPagination', () => {
    test('페이지네이션이 렌더링된다.', () => {
    render(<ScrapBookPagination currentPage={2} previousPage={1} totalPages={3} nextPage={3} />)
    expect(screen.getByRole('link', { name: '페이지 1' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '페이지 2 (현재 페이지)' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '페이지 3' })).toBeInTheDocument()
    })

     test('현재 페이지가 첫 페이지인 경우 이전 버튼이 비활성화된다.', () => {
        render(<ScrapBookPagination currentPage={1} previousPage={1} totalPages={3} nextPage={2}/>)
        expect(screen.queryByRole('link', { name: '이전 페이지' })).not.toBeInTheDocument()
    })
    
     test('현재 페이지가 마지막 페이지인 경우 다음 버튼이 비활성화된다.', () => {
        render(<ScrapBookPagination currentPage={3} previousPage={2} totalPages={3} nextPage={3}/>)
        expect(screen.queryByRole('link', { name: '다음 페이지' })).not.toBeInTheDocument()
    })  

    test('현재 페이지에는 aria-current가 적용된다.', () => {
        render(<ScrapBookPagination currentPage={2} previousPage={1} totalPages={3} nextPage={2}/>)
        expect(screen.getByRole('link', { name: '페이지 2 (현재 페이지)' })).toHaveAttribute('aria-current', 'page')
    })
    test('페이지가 1개인 경우 이전/다음 버튼이 보이지 않는다.', () => {
        render(<ScrapBookPagination currentPage={1} previousPage={1} totalPages={1} nextPage={1}/>)
        expect(screen.queryByRole('link', {name: '이전 페이지'})).not.toBeInTheDocument()
        expect(screen.queryByRole('link', {name: '다음 페이지'})).not.toBeInTheDocument()
    })  
    test('페이지네이션에 접근성 속성이 올바르게 적용된다.', () => {
        render(<ScrapBookPagination currentPage={2} previousPage={1} totalPages={3} nextPage={3}/>)
        expect(screen.getByRole('navigation', { name: '스크랩북 목록 페이지네이션' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: '이전 페이지' })).toHaveAttribute('aria-label', '이전 페이지')
        expect(screen.getByRole('link', { name: '다음 페이지' })).toHaveAttribute('aria-label', '다음 페이지')
    })  
    test('페이지 번호 링크에 올바른 href가 적용된다.', () => {
        render(<ScrapBookPagination currentPage={2} previousPage={1} totalPages={3} nextPage={3}/>)
        expect(screen.getByRole('link', { name: '페이지 1' })).toHaveAttribute('href', '?page=1')
        expect(screen.getByRole('link', { name: '페이지 2 (현재 페이지)' })).toHaveAttribute('href', '?page=2')
        expect(screen.getByRole('link', { name: '페이지 3' })).toHaveAttribute('href', '?page=3')
    })
    test('이전/다음 버튼에 올바른 href가 적용된다.', () => {
        render(<ScrapBookPagination currentPage={2} previousPage={1} totalPages={3} nextPage={3}/>)
        expect(screen.getByRole('link', { name: '이전 페이지' })).toHaveAttribute('href', '?page=1')
        expect(screen.getByRole('link', { name: '다음 페이지' })).toHaveAttribute('href', '?page=3')
    })  
               
})
