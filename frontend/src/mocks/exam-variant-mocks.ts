import type { NewQuestionModalValues } from '@/types/exam-variant'

//목데이터
export const MOCK_DATA: NewQuestionModalValues = {
  data: {
    questionNewId: 2,
    category: 'GEO',
    type: 'MCQ',
    passage: '1 + 1을 구하시오.',

    options: [
      {
        order: 1,
        text: '-1',
      },
      {
        order: 2,
        text: '0',
      },
      {
        order: 3,
        text: '2',
      },
      {
        order: 4,
        text: '-2',
      },
      {
        order: 5,
        text: '10',
      },
    ],
    answer: 3,
    analysis:
      '1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.1+1은 2입니다.',
  },
}
