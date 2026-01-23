'use client'

import { Streamdown } from 'streamdown'
import { math } from '@streamdown/math'
import { cjk } from '@streamdown/cjk'
import 'katex/dist/katex.min.css'

interface StreamdownRendererProps {
  content: string
}

export default function StreamdownRenderer({ content }: StreamdownRendererProps) {
  if (!content) return null

  return <Streamdown plugins={{ math, cjk }}>{content}</Streamdown>
}
