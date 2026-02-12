import type { Metadata } from 'next'
import localFont from 'next/font/local'
import './globals.css'
import 'katex/dist/katex.min.css'
import QueryProvider from '@/providers/QueryProvider'
import { Toaster } from '@/components/common/Toast'
import { GoogleAnalytics } from '@next/third-parties/google'

const pretendard = localFont({
  src: './fonts/PretendardVariable.woff2',
  variable: '--font-pretendard',
  display: 'swap',
  weight: '45 920',
})

export const metadata: Metadata = {
  metadataBase: new URL('https://opener.deving.xyz'),
  title: {
    default: '오프너',
    template: '%s | 오프너',
  },
  description: '시험 문제 풀이와 오답 정리를 통해 학습 흐름을 관리하는 문제 풀이 서비스',
  keywords: ['오프너', 'Opener', '수험생', '수학', '문제풀이', '오답노트', '학습관리'],
  openGraph: {
    title: '오프너',
    description: '문제 풀이부터 오답 정리까지, 학습의 흐름을 여는 AI 서비스',
    type: 'website',
    images: [
      {
        url: '/image/logo_h56_p.svg',
        width: 1200,
        height: 630,
        alt: '오프너',
      },
    ],
  },
  robots: {
    index: false,
    follow: false,
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`${pretendard.variable} antialiased`}>
        <QueryProvider>{children}</QueryProvider>
        <Toaster />
      </body>
      <GoogleAnalytics gaId="G-NQQL9SM6VP" />
    </html>
  )
}
