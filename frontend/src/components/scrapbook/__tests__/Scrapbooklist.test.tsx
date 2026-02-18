import {afterEach, describe, expect, test, vi} from "vitest";
import ScrapBookList from "../ScrapBookList";
import { render, screen } from "@testing-library/react";
import useScrapBookList from "@/hooks/scrapbook/use-scrapbook-list";

const mockScrapBookListData = {
     examYear: 2026,
            examType:{
                code:'1',
                name:"수학능력시험"},
            questionResults:{
                content:[
                    {   
                    questionResultId: 5,
                    category: {  
                        code:'PROB',
                        name: "확률과 통계"
                    },
                    questionNo: 5,
                    passage : "함수 $f(x)=(x+2)\\left(2 x^2-x-2\\right)$ 에 대하여 $f^{\\prime}(1)$ 의 값은?",
                    openerUsedAt: "2026-01-19T23:21:16"
                },
            ],
            page:1,
            size:1,
            totalElements:3,
            totalPages:1,
            last:true,
          }
        
}

vi.mock('next/navigation', () => ({
    useRouter: () => ({
        replace: vi.fn(),
    }),
    useParams: () => ({
        examId:"1",
    }),
    useSearchParams: () => new URLSearchParams("page=1"),
    usePathname: () => "/scrapbook/1"
}))

vi.mock('../ScrapBookListItem', () => ({
 default: () => <div>mockScrapBookListItem</div> 
}))

vi.mock('../ScrapBookPagination', () => ({
    default: () => <div>mockScrapBookPagination</div>
}))

vi.mock('@/hooks/scrapbook/use-scrapbook-list')
const mockedHook = vi.mocked(useScrapBookList)

type MockUseScrapBookListReturnType = ReturnType<typeof useScrapBookList>

const createMockUseScrapBookListReturn = (overrides: Partial<MockUseScrapBookListReturnType> = {}): MockUseScrapBookListReturnType => ({
    data: mockScrapBookListData,
    isLoading: false,
    isError: false,
    ...overrides,
} as MockUseScrapBookListReturnType)

describe('ScrapBookList', () => {
    afterEach(() => {
  vi.clearAllMocks()
})

test('스크랩북 리스트에 에러가 발생하면 에러 메시지가 보인다.', () => {
    mockedHook.mockReturnValue(createMockUseScrapBookListReturn({
        isError: true,
    })  )
   expect(() => render(<ScrapBookList/>)).toThrow()
})

test('로딩 중일 때 로딩 메시지가 보인다.', () => {
    mockedHook.mockReturnValue(createMockUseScrapBookListReturn({
        isLoading: true,
    }))
    render(<ScrapBookList/>)
    expect(screen.getByText('데이터를 불러오는 중...')).toBeInTheDocument()
})

test('스크랩북 리스트 데이터가 렌더링 된다.', () => {
    mockedHook.mockReturnValue(createMockUseScrapBookListReturn())
    render(<ScrapBookList/>)
    expect(screen.getByText('mockScrapBookListItem')).toBeInTheDocument()
    expect(screen.getByText('mockScrapBookPagination')).toBeInTheDocument()
})})