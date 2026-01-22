import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/login', '/register']

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl
  const accessToken = request.cookies.get('accessToken')

  if (pathname === '/verify') {
    return NextResponse.next()
  }

  // 역방향 가드: 로그인한 유저가 로그인/회원가입 접근 시
  if (accessToken && PUBLIC_PATHS.includes(pathname)) {
    return NextResponse.redirect(new URL('/', request.url))
  }

  // 순방향 가드: 퍼블릭 경로가 아니고, AT가 없는 경우 (verify로 위임)
  if (!accessToken && !PUBLIC_PATHS.includes(pathname)) {
    // /verify 자체는 무한 루프 방지를 위해 예외 처리
    if (pathname.includes('/verify')) return NextResponse.next()

    const callbackUrl = encodeURIComponent(`${pathname}${search}`)
    return NextResponse.redirect(new URL(`/verify?callback=${callbackUrl}`, request.url))
  }

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
