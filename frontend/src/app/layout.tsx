import type { Metadata } from 'next'
import localFont from 'next/font/local'
import './globals.css'
import 'katex/dist/katex.min.css'
import QueryProvider from '@/providers/QueryProvider'

const pretendard = localFont({
  src: './fonts/PretendardVariable.woff2',
  variable: '--font-pretendard',
  display: 'swap',
  weight: '45 920',
})

export const metadata: Metadata = {
  title: 'Opener',
  description: '오프너 홈페이지',
  keywords: ['오프너', 'Opener', '수험생', '수학'],
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`${pretendard.variable}  antialiased`}>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  )
}
