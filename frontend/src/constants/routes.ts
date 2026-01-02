export const ROUTES = {
  DASHBOARD: '/',
  EXAM: '/exam',
  SCRAPBOOK: '/scrapbook',
  MY_PAGE: '/mypage',
} as const

export type RouteKey = keyof typeof ROUTES
export type RoutePath = (typeof ROUTES)[RouteKey]
