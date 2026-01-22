import katex from 'katex'

type RenderLatexOptions = {
  blockDisplayMode?: boolean
}
export default function renderLatex(text: string, option: RenderLatexOptions = {}): string {
  const { blockDisplayMode = true } = option

  return (
    text
      // 블록 수식 처리 (\[...\])
      .replace(/\\\[([\s\S]+?)\\\]/g, (_, latex) => {
        return katex.renderToString(latex.trim(), {
          displayMode: blockDisplayMode,
          throwOnError: false,
        })
      })

      // 인라인 수식 처리 (\(...\))
      .replace(/\\\(([\s\S]+?)\\\)/g, (_, latex) => {
        return katex.renderToString(latex.trim(), { displayMode: false, throwOnError: false })
      })
  )
}
