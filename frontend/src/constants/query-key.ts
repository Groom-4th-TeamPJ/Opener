// NOTE: 쿼리키 정리 필요

import { ExamRequestParams } from '@/types/exam'

export const QUERY_KEYS = {
  // Auth 관련
  AUTH: {
    ROOT: ['auth'] as const,
    REGISTER: ['auth', 'register'] as const,
    LOGIN: ['auth', 'login'] as const,
    LOGOUT: ['auth', 'logout'] as const,
    REFRESH: ['auth', 'refresh'] as const,
    ME: ['auth', 'me'] as const,
  },

  USER: {
    ROOT: ['user'] as const,
    CAN: ['user', 'can'] as const,
  },

  DASHBOARD: {
    ROOT: ['dashboard'] as const,
    SUMMARY: ['dashboard', 'summary'] as const,
    CORRECT_RATE: ['dashboard', 'rate'] as const,
    TOTAL_SOLVED: ['dashboard', 'solved'] as const,
    TOTAL_TIME_SPENT: ['dashboard', 'time-spent'] as const,
  },

  // 문제풀이 관련
  EXAM: {
    ROOT: ['exam'] as const,
    // 현재 진행 중인(조회된) 시험지 데이터
    CURRENT: (params: ExamRequestParams) =>
      ['exam', 'current', params.examYear, params.category, params.examType] as const,
    // 답안 제출
    SUBMIT: ['exam', 'question', 'submit'] as const,
    // 답안 제출 결과 (questionId별)
    SUBMIT_RESULT: (questionId: number) => ['exam', 'question', 'submit', questionId] as const,
    // 학습 결과 (examResultId별)
    RESULT: (examResultId: number) => ['exam', 'result', examResultId] as const,
    // 오프너 분석
    ANALYZE: ['exam', 'question', 'analyze'] as const,
    // 변형 문제 생성
    GENERATE: ['exam', 'question', 'generate'] as const,
    CHAT: ['exam', 'chat'] as const,
    SOCKET: ['exam', 'socket'] as const,
  },

  SCRAPBOOK: {
    ROOT: ['scrapbook'] as const,
    LIST: ['scrapbook', 'list'] as const,
    DETAIL: ['scrapbook', 'detail'] as const,
    SUMMARY: ['scrapbook', 'summary'] as const,
  },
} as const
