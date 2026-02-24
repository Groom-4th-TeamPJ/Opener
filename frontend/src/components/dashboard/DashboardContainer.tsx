'use client'

import GreetingSection from '@/components/dashboard/GreetingSection'
import StatsPanel from '@/components/dashboard/StatsPanel'
import useGetMetrics from '@/hooks/dashboard/use-get-metrics'
import { useEffect } from 'react'

export default function DashboardContainer() {
  const { data, isLoading, isError } = useGetMetrics()

  if (isError) throw new Error()
  const isEmpty = !data || data.totalQuestionsSolvedCount === 0
  const safeData = isEmpty ? null : data

  useEffect(() => {
    if (sessionStorage.getItem('pending_oauth') === 'true') {
      sessionStorage.removeItem('pending_oauth')
    }
  }, [])

  return (
    <main className="w-full max-w-6xl px-4 mx-auto">
      <GreetingSection name={isLoading || !data ? '회원' : data.userName} />

      <section className="lg:flex justify-center gap-6 space-y-4 lg:space-y-0">
        <StatsPanel metrics={safeData} isLoading={isLoading} />
      </section>
    </main>
  )
}
