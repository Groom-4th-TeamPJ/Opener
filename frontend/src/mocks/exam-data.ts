import type { ExamResponse } from '@/types/exam'

export const mockExamData: ExamResponse = {
  exam: {
    examId: 1,
    examYear: 2025,
    examType: { code: 'CSAT', name: '수능' },
  },
  questions: [
    {
      questionId: 33,
      questionNo: 1,
      category: { code: 'GEO', name: '기하' },
      point: 4,
      type: 'MCQ',
      passages: [
        {
          order: 1,
          type: 'TEXT',
          text: '그림과 같이 $\\overline{\\mathrm{AB}}=3, \\overline{\\mathrm{BC}}=4$ 이고 $\\angle \\mathrm{B}=\\frac{\\pi}{2}$ 인 직각삼각형 ABC 가 있다. 선분 AB 를 2:1로 내분하는 점을 D , 점 A 를 중심으로 하고 반지름의 길이가 $\\overline{\\mathrm{AD}}$ 인 원이 선분 AC 와 만나는 점을 E , 직선 AB 가 이 원과 만나는 점 중 D 가 아닌 점을 F 라 하고, 호 EF 위의 점 G 를 $\\overline{\\mathrm{CG}}=2 \\sqrt{6}$ 이 되도록 잡는다. 세 점 $\\mathrm{C}, \\mathrm{E}, \\mathrm{G}$ 를 지나는 원 위의 점 H 가 $\\angle \\mathrm{HCG}=\\angle \\mathrm{BAC}$ 를 만족시킬 때, 선분 GH 의 길이는?',
          url: null,
        },
      ],
      options: [
        { order: 1, text: '$\\frac{\\sqrt{6}}{2}$' },
        { order: 2, text: '$\\sqrt{6}$' },
        { order: 3, text: '$\\frac{3\\sqrt{6}}{2}$' },
        { order: 4, text: '$2\\sqrt{6}$' },
        { order: 5, text: '$\\frac{5\\sqrt{6}}{2}$' },
      ],
      answer: 2,
    },
    {
      questionId: 31,
      questionNo: 2,
      category: { code: 'CALC', name: '미적분' },
      point: 2,
      type: 'MCQ',
      passages: [
        {
          order: 1,
          type: 'TEXT',
          text: '함수',
          url: null,
        },
        {
          order: 2,
          type: 'TEXT',
          text: '$$f(x)= \\begin{cases}3 x-2 & (x<1) \\\\ x^{2}-3 x+a & (x \\geq 1)\\end{cases}$$',
          url: null,
        },
        {
          order: 3,
          type: 'TEXT',
          text: '이 실수 전체의 집합에서 연속일 때, 상수 $a$ 의 값은?',
          url: null,
        },
      ],
      options: [
        { order: 1, text: '1' },
        { order: 2, text: '2' },
        { order: 3, text: '3' },
        { order: 4, text: '4' },
        { order: 5, text: '5' },
      ],
      answer: 2,
    },
    {
      questionId: 32,
      questionNo: 3,
      category: { code: 'GEO', name: '기하' },
      point: 3,
      type: 'FRQ',
      passages: [
        {
          order: 1,
          type: 'TEXT',
          text: '수열 $\\left\\{a_{n}\\right\\}$ 에 대하여 $\\sum_{k=1}^4\\left(2 a_{k}-k\\right)=0$ 일 때, $\\sum_{k=1}^4 a_k$ 의 값은?',
          url: null,
        },
      ],
      options: null,
      answer: 110,
    },
  ],
}
