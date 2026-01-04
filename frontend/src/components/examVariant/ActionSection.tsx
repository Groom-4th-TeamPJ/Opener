import type { ActionSectionProps } from '@/types/exam-variant'
import Button from '../common/Button'

export default function ActionSection({
  isSubmitted,
  selected,
  onSubmit,
  onBack,
}: ActionSectionProps) {
  return (
    <>
      {!isSubmitted ? (
        <Button className="flex-1" disabled={selected === null} onClick={onSubmit}>
          제출하기
        </Button>
      ) : (
        <Button className="flex-1" onClick={onBack}>
          원래 문제로 돌아가기
        </Button>
      )}
    </>
  )
}
