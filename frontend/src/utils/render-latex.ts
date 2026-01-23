import katex from 'katex'
import { marked } from 'marked'

type RenderLatexOptions = {
  blockDisplayMode?: boolean
}

export default function renderLatex(text: string, option: RenderLatexOptions = {}): string {
  if (!text) return ''

  const { blockDisplayMode = true } = option

  const render = (latex: string, displayMode: boolean) =>
    katex.renderToString(latex.trim(), {
      displayMode,
      throwOnError: false,
      trust: true,
    })

  let processed = text

  /**
   * 1. Block math
   * $$ ... $$
   */
  processed = processed.replace(/\$\$([\s\S]+?)\$\$/g, (_, latex) =>
    render(latex, blockDisplayMode)
  )

  /**
   * 2. Block math
   * \[ ... \]
   */
  processed = processed.replace(/\\\[([\s\S]+?)\\\]/g, (_, latex) =>
    render(latex, blockDisplayMode)
  )

  /**
   * 3. Inline math
   * \( ... \)
   */
  processed = processed.replace(/\\\(([\s\S]+?)\\\)/g, (_, latex) => render(latex, false))

  /**
   * 4. Inline math
   * $ ... $
   */
  processed = processed.replace(/(?<!\$)\$(?!\$)([^$\n]+?)\$(?!\$)/g, (_, latex) =>
    render(latex, false)
  )

  /**
   * 5. Markdown 처리
   */
  return marked.parse(processed, {
    breaks: true,
    gfm: true,
  }) as string
}
