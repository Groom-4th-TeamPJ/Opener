'use client'
import Link from 'next/link'
import Image from 'next/image'
import Button from './Button'
import { usePathname, useRouter } from 'next/navigation'
import useLogout from '@/hooks/auth/use-logout'
import { useState } from 'react'
import cn from '@/utils/cn'
import SolidCanIcon from '@/components/icons/SolidCanIcon'
import useCanCount from '@/hooks/header/use-can-count'
import { ROUTES } from '@/constants/routes'
import { useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-key'

const MENU = [
  { label: '대시보드', href: ROUTES.DASHBOARD },
  { label: '문제풀이', href: ROUTES.EXAM },
  { label: '스크랩북', href: ROUTES.SCRAPBOOK },
  { label: '마이페이지', href: ROUTES.MY_PAGE },
]

export default function Header() {
  const pathName = usePathname()
  const { mutateAsync: logout } = useLogout()
  const [isOpen, setIsOpen] = useState(false)
  const { data } = useCanCount()
  const queryClient = useQueryClient()
  const router = useRouter()
  const toggleMenu = () => {
    setIsOpen((prev) => !prev)
  }
  const closeMenu = () => {
    setIsOpen(false)
  }

  const handleLogout = async () => {
    queryClient.setQueryDefaults(QUERY_KEYS.USER.CAN, { enabled: false })

    try {
      await logout()
    } finally {
      queryClient.clear()
      router.replace('/login')
    }
  }

  return (
    <header className="bg-background sticky top-0 z-50 px-4">
      <div className="mx-auto max-w-6xl flex items-center h-14 justify-between ">
        <div className="flex gap-6 items-center">
          {/* mobile */}
          <div
            className="lg:hidden"
            onClick={toggleMenu}
            aria-label={isOpen ? '메뉴 닫기' : '메뉴 열기'}
          >
            <Image
              src={isOpen ? '/icons/close.svg' : '/icons/hamburger.svg'}
              alt={isOpen ? '메뉴 닫기' : '메뉴 열기'}
              width={24}
              height={24}
            />
          </div>

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
        </div>

        <div className="flex items-center gap-10">
          <nav className="hidden lg:flex gap-8">
            {MENU.map((menuItem) => {
              const isActive = pathName === menuItem.href
              return (
                <Link
                  key={menuItem.href}
                  href={menuItem.href}
                  className={cn(
                    'text-neutral-600',
                    isActive ? 'text-neutral-900 font-bold' : 'hover:font-bold'
                  )}
                >
                  {menuItem.label}
                </Link>
              )
            })}
          </nav>
        </div>

        <div className=" flex items-center gap-2">
          <div className="flex gap-1.5 items-center px-4">
            <SolidCanIcon className="text-primary-600" />

            <span className="font-bold">{data?.currentCan ?? 0}</span>
          </div>

          <Button
            variant="ghost"
            size="sm"
            className="w-16 p-0 border border-neutral-200"
            onClick={handleLogout}
          >
            로그아웃
          </Button>
        </div>
        {/* 모바일 메뉴 */}
        {isOpen && (
          <div className="lg:hidden absolute top-14 right-0 left-0 bg-white ">
            <nav className="flex flex-col ">
              {MENU.map((menuItem) => (
                <Link
                  key={menuItem.label}
                  href={menuItem.href}
                  onClick={closeMenu}
                  className="h-14 py-4 px-4"
                >
                  {menuItem.label}
                </Link>
              ))}
            </nav>
          </div>
        )}
      </div>
    </header>
  )
}
