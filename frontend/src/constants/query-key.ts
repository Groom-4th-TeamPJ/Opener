// NOTE: 쿼리키 정리 필요

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

  EXAM: {
    ROOT: ['exam'] as const,
    SUBMIT: ['exam', 'question', 'submit'] as const,
    ANALYZE: ['exam', 'question', 'analyze'] as const,
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
