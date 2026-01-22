import katex from 'katex'
import { marked } from 'marked'

type RenderLatexOptions = {
  blockDisplayMode?: boolean
}
export default function renderLatex(text: string, option: RenderLatexOptions = {}): string {
  if (!text) return ''

  const { blockDisplayMode = true } = option

  // 1. LaTeX 수식 치환
  const latexProcessed = text
    .replace(/\\\[([\s\S]+?)\\\]/g, (_, latex) => {
      return katex.renderToString(latex.trim(), {
        displayMode: blockDisplayMode,
        throwOnError: false,
      })
    })
    .replace(/\\\(([\s\S]+?)\\\)/g, (_, latex) => {
      return katex.renderToString(latex.trim(), {
        displayMode: false,
        throwOnError: false,
      })
    })

  // 2. 마크다운 변환
  // marked.parse는 최신 버전에서 기본적으로 string을 반환하도록 설정되어 있습니다.
  return marked.parse(latexProcessed, {
    breaks: true,
    gfm: true,
  }) as string
}
