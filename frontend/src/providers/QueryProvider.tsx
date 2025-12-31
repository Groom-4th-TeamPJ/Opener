import { ClientQueryProvider } from '@/providers/QueryProvider.client'
import type { ReactNode } from 'react'

export default function QueryProvider({ children }: { children: ReactNode }) {
  return <ClientQueryProvider>{children}</ClientQueryProvider>
}
