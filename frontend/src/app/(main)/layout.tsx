import Header from '@/components/common/Header'
import { ReactNode } from 'react'

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh flex flex-col">
      <Header />
      <main className="flex-1  w-full mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  )
}
