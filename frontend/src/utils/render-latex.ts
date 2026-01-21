import katex from 'katex'

export default function renderLatex(text: string): string {
  return (
    text
      // 블록 수식 처리 (\[...\])
      .replace(/\\\[([\s\S]+?)\\\]/g, (_, latex) => {
        return katex.renderToString(latex.trim(), { displayMode: true, throwOnError: false })
      })

      // 인라인 수식 처리 (\(...\))
      .replace(/\\\(([\s\S]+?)\\\)/g, (_, latex) => {
        return katex.renderToString(latex.trim(), { displayMode: false, throwOnError: false })
      })
  )
}
