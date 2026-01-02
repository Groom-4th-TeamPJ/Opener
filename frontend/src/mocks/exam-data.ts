import type { ExamResponse } from '@/types/exam'

export const mockExamData: ExamResponse = {
  exam: {
    examId: 1,
    year: 2025,
    examType: { code: 'CSAT', name: '수능' },
  },
  questions: [
    {
      questionId: 31,
      order: 1,
      category: { code: 'CALC', name: '미적분' },
      difficulty: 'EASY',
      point: 2,
      type: 'MCQ',
      passages: [
        {
          order: 1,
          type: 'text',
          text: '다음 극한값을 구하시오.',
          url: null,
        },
      ],
      options: [
        { order: 1, text: '-1' },
        { order: 2, text: '0' },
        { order: 3, text: '1' },
        { order: 4, text: '2' },
        { order: 5, text: '3' },
      ],
      answer: 2,
    },
    {
      questionId: 32,
      order: 2,
      category: { code: 'GEO', name: '기하' },
      difficulty: 'MEDIUM',
      point: 3,
      type: 'FRQ',
      passages: [
        {
          order: 1,
          type: 'text',
          text: '좌표평면에서 두 점 A(1, 2), B(3, 4) 사이의 거리를 구하시오.',
          url: null,
        },
      ],
      options: null,
      answer: 110,
    },
    {
      questionId: 33,
      order: 3,
      category: { code: 'PROB', name: '확률과 통계' },
      difficulty: 'HARD',
      point: 3,
      type: 'MCQ',
      passages: [
        {
          order: 1,
          type: 'text',
          text: '주머니에 빨간 공 3개, 파란 공 2개가 들어있다. 임의로 2개를 뽑을 때, 2개 모두 빨간 공일 확률은?',
          url: null,
        },
      ],
      options: [
        { order: 1, text: '1/10' },
        { order: 2, text: '3/10' },
        { order: 3, text: '1/5' },
        { order: 4, text: '2/5' },
        { order: 5, text: '3/5' },
      ],
      answer: 1,
    },
  ],
}
