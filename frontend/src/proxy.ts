import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/login', '/register']

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  const accessToken = request.cookies.get('accessToken')?.value
  const refreshToken = request.cookies.get('refreshToken')?.value
  const hasToken = accessToken || refreshToken

  // 공개 경로 접근 시: 토큰이 있다면 대시보드로 리다이렉트 (로그인했는데 로그인 페이지 가는 거 방지)
  if (PUBLIC_PATHS.some((path) => pathname.startsWith(path))) {
    if (hasToken) {
      return NextResponse.redirect(new URL('/', request.url))
    }
    return NextResponse.next()
  }

  // 토큰이 없다면 로그인 페이지로 리다이렉트
  if (!hasToken) {
    return NextResponse.redirect(new URL('/login', request.url))
  }

  // 그 외의 경우 정상 진행
  return NextResponse.next()
}

// 미들웨어가 실행될 경로 설정
export const config = {
  matcher: [
    /*
     * 1. /api 로 시작하는 경로 제외 (API 유틸리티가 처리함)
     * 2. _next/static, _next/image 등 정적 자원 제외
     * 3. favicon.ico 등 공통 파일 제외
     * 4. 이미지(png, jpg 등) 파일 제외
     */
    '/((?!api|_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml|manifest.webmanifest|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico|css|js|map|txt|xml|json|woff2?|ttf|otf|eot)$).*)',
  ],
}
