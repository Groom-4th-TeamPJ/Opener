// TODO: API 명세 주소와 일치

export const API_PATHS = {
  AUTH: {
    FORM_LOGIN: '/auth/form-login',
    LOGOUT: '/auth/logout',
    FORM_REGISTER: '/auth/form-signup',
    REFRESH: '/auth/refresh',
  },
  EXAM: {
    ROOT: '/exam',
    ANALYSIS: '/exam/analysis',
    GENERATE: '/exam/generate',
    SAVE_CHAT: '/exam/save-chat',
  },
  CHAT: {
    CONNECT: '/chat/connect',
    DISCONNECT: '/chat/disconnect',
    MESSAGE: '/chat/message',
  },
} as const
