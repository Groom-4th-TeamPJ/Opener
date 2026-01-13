import type { ScrapbookQuestion } from '@/types/exam'

export const mockScrapbookChatData: { data: ScrapbookQuestion } = {
  data: {
    questionId: 31,
    questionNo: 1,
    examYear: 2025,
    examType: { code: 'CSAT', name: '수학능력시험' },
    createdAt: '2025-01-13T14:30:00Z',
    point: 2,
    type: 'MCQ' as const,
    passages: [
      {
        order: 1,
        type: 'TEXT' as const,
        text: '다음 극한값을 구하시오.\n\nlim(x→2) (x² - 4)/(x - 2)',
        url: null,
      },
      {
        order: 2,
        type: 'IMAGE' as const,
        text: null,
        url: 'https://placehold.co/400x400/e0e0e0/666?text=Math+Graph',
      },
      {
        order: 3,
        type: 'TEXT' as const,
        text: '위 식을 간단히 하면?',
        url: null,
      },
    ],
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
        text: '1',
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
    select: 2,
    chat: [
      {
        order: 1,
        role: 'ASSISTANT',
        content:
          '2번을 선택했네요.\n\n왜 0이 정답이라고 생각하셨나요?\n어떤 근거로 그렇게 판단하셨는지 설명해주세요!',
        timestamp: '오후 2:30',
      },
      {
        order: 2,
        role: 'USER',
        content: 'x가 2에 가까워지면 분자가 0이 되니까 전체 값도 0이 될 것 같아서요.',
        timestamp: '오후 2:31',
      },
      {
        order: 3,
        role: 'ASSISTANT',
        content:
          '좋은 관찰이에요! 하지만 분자가 0이 되는 것과 함께 분모도 0이 되는 것을 확인해야 합니다.\n\n(x² - 4)는 (x+2)(x-2)로 인수분해할 수 있어요. 그러면 분모의 (x-2)와 약분되어 결국 lim(x→2) (x+2) = 4가 됩니다.\n\n따라서 정답은 1번이 아니라 다른 값이 되는 거죠!',
        timestamp: '오후 2:31',
      },
      {
        order: 4,
        role: 'USER',
        content: '아, 인수분해를 해야 했군요! 그럼 답은 4인가요?',
        timestamp: '오후 2:32',
      },
      {
        order: 5,
        role: 'ASSISTANT',
        content:
          '정확해요! x+2에 x=2를 대입하면 4가 됩니다.\n\n이런 유형의 문제는 0/0 꼴의 부정형일 때 인수분해나 유리화 같은 방법으로 먼저 식을 정리한 후 극한값을 구해야 한다는 점을 기억하세요!',
        timestamp: '오후 2:32',
      },
      {
        order: 6,
        role: 'USER',
        content: '아, 인수분해를 해야 했군요! 그럼 답은 4인가요?',
        timestamp: '오후 2:32',
      },
      {
        order: 7,
        role: 'ASSISTANT',
        content:
          '정확해요! x+2에 x=2를 대입하면 4가 됩니다.\n\n이런 유형의 문제는 0/0 꼴의 부정형일 때 인수분해나 유리화 같은 방법으로 먼저 식을 정리한 후 극한값을 구해야 한다는 점을 기억하세요!',
        timestamp: '오후 2:32',
      },
    ],
  },
}
