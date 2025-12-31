'use client'
import Link from 'next/link'
import Image from 'next/image'
import Button from './Button'
import { useRouter, usePathname } from 'next/navigation'
import { X } from 'lucide-react'

const MENU = [
  { label: '대시보드', href: '/' },
  { label: '문제풀이', href: '/exam' },
  { label: '스크랩북', href: '/scrapbook' },
  { label: '마이페이지', href: '/mypage' },
]

export default function Header() {
  const router = useRouter()
  const pathName = usePathname()

  const handleLogout = () => {
    // 백엔드 로그아웃 API 호출 및 토큰 삭제 로직 추가 예정
    router.replace('/login')
  }

  return (
    <header className="bg-white sticky top-0 z-50 border-b border-gray-300">
      <div className=" mx-auto max-w-6xl flex items-center px-4 h-16 justify-between">
        <div className="flex items-center gap-10">
          <Link href="/">
            <Image
              src="/image/logo_h32_p.svg"
              width={50}
              height={50}
              alt="오프너 로고"
              className="shrink-0 h-8 w-auto"
              fetchPriority="high"
              loading="eager"
            />
          </Link>
          <nav className="flex gap-8">
            {MENU.map((menuItem) => {
              const isActive = pathName === menuItem.href
              return (
                <Link
                  key={menuItem.href}
                  href={menuItem.href}
                  className={`${isActive ? 'font-bold' : ' hover:font-bold'}`}
                >
                  {menuItem.label}
                </Link>
              )
            })}
          </nav>
        </div>

        <div className=" flex items-center gap-2">
          <Image
            src="/image/symbol_24_p.svg"
            alt="오프너의 재화 캔"
            width={48}
            height={48}
            className="shrink-0 h-5 w-auto"
            fetchPriority="auto"
          />
          <X />
          <span className="font-bold">{/* 캔 개수 */}10</span>
          <Button size="sm" className="bg-primary-600" onClick={handleLogout}>
            로그아웃
          </Button>
        </div>
      </div>
    </header>
  )
}
