// TODO: API 명세 주소와 일치

export const API_PATHS = {
  AUTH: {
    FORM_LOGIN: '/auth/form-login',
    OAUTH_LOGIN: '/oauth2/authorization/kakao',
    LOGOUT: '/auth/logout',
    FORM_REGISTER: '/auth/form-signup',
    OAUTH_REGISTER: '/auth/oauth-signup',
    REFRESH: '/auth/refresh',
  },
  DASHBOARD: {
    SUMMARY: '/dashboard/summary',
    ACCURACY: '/dashboard/metrics/correct-rate',
    TOTAL_SOLVED: '/dashboard/metrics/total-questions-solved',
    TOTAL_STUDY: '/dashboard/metrics/total-learning-time',
  },
  EXAM: {
    ROOT: '/exams',
    SUBMIT: '/exams/results',
    ANALYSIS: '/exams/analysis',
    GENERATE: '/exams/generate',
    SAVE_CHAT: '/exams/save-chat',
  },
  CHAT: {
    CONNECT: '/chat/connect',
    DISCONNECT: '/chat/disconnect',
    MESSAGE: '/chat/message',
  },
  USERS: {
    ME: '/users/me/cans/count',
  },
  SCRAPBOOK: {
    LIST: '/scrapbooks',
    EXAM_LIST: '/scrapbooks/filters',
  },
} as const
