import { ModalContent } from '../common/Modal'

type AnalysisProps = {
  analysis: unknown
  isSubmitted: boolean
  isCorrect: boolean | null
}
export default function AnalysisSection({ analysis, isSubmitted, isCorrect }: AnalysisProps) {
  if (!isSubmitted || isCorrect) return null

  if (typeof analysis !== 'string') {
    return (
      <ModalContent className="px-6 py-0">
        <div className="bg-neutral-50 rounded-lg p-4 text-sm text-neutral-500">
          해설이 아직 준비되지 않았습니다.
        </div>
      </ModalContent>
    )
  }

  return (
    <>
      {isSubmitted && !isCorrect && (
        <ModalContent className="px-6 py-0">
          <div className="bg-neutral-50 rounded-lg p-4 text-sm">{analysis}</div>
        </ModalContent>
      )}
    </>
  )
}
