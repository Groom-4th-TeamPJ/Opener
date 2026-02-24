'use client'

import { Streamdown } from 'streamdown'
import { createMathPlugin } from '@streamdown/math'
import { cjk } from '@streamdown/cjk'
import 'katex/dist/katex.min.css'

const math = createMathPlugin({
  singleDollarTextMath: true,
})

interface StreamdownRendererProps {
  content: string
}

export default function StreamdownRenderer({ content }: StreamdownRendererProps) {
  if (!content) return null

  return <Streamdown plugins={{ math, cjk }}>{content}</Streamdown>
}
