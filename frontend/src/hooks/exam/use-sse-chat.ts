/* eslint-disable no-console */
import { useEffect } from 'react'
import { API_PATHS } from '@/constants/api-path'

interface UseSSEChatProps {
  sessionId: number
}

export function useSSEChat({ sessionId }: UseSSEChatProps): void {
  useEffect(() => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL
    const url = `${baseUrl}${API_PATHS.CHAT.CONNECT}?session=${sessionId}`

    console.log('[SSE] Connecting to:', url)

    const eventSource = new EventSource(url, {
      withCredentials: true,
    })

    eventSource.onopen = () => {
      console.log('[SSE] Connected successfully')
    }

    eventSource.onerror = (err) => {
      console.error('[SSE] Connection failed:', err)
      eventSource.close()
    }

    return () => {
      eventSource.close()
    }
  }, [sessionId])
}
