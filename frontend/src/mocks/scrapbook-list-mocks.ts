import { ScrapBookListResponse } from '@/types/scrapbook-list.type'

export const ScrapBookListMocks: ScrapBookListResponse = {
  status: 'success',
  code: 200,
  message: '성공',
  data: {
    examYear: 2022,
    examType: { code: 'M06', name: '6모' },
    questionResults: [
      {
        questionResultId: 2,
        category: { code: 'ALG', name: '수학I + 수학II' },
        questionNo: 3,
        passage: '23. 닫힌구간 [0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 3,
        category: { code: 'ALG', name: '기하' },
        questionNo: 10,
        passage: '12. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 4,
        category: { code: 'ALG', name: '기하' },
        questionNo: 10,
        passage: '23. 닫힌구간 [0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 5,
        category: { code: 'ALG', name: '기하' },
        questionNo: 10,
        passage: '13. 닫힌구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 6,
        category: { code: 'ALG', name: '확률' },
        questionNo: 10,
        passage: '12. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 7,
        category: { code: 'ALG', name: '기하' },
        questionNo: 10,
        passage: '114. 닫힌구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 8,
        category: { code: 'ALG', name: '미적분' },
        questionNo: 10,
        passage:
          '16. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?16. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 9,
        category: { code: 'ALG', name: '미적분' },
        questionNo: 10,
        passage: '19. 닫힌구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 10,
        category: { code: 'ALG', name: '기하' },
        questionNo: 10,
        passage: '12. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
      {
        questionResultId: 11,
        category: { code: 'ALG', name: '미적분' },
        questionNo: 10,
        passage: '12. 열린구간[0, 4]에서 함수 f(x)=x³6x²+9x+a의 최댓값이 12일 때 최소값은?',
        createdAt: '2025.12.31 15:20',
      },
    ],
  },
  error: null,
}
