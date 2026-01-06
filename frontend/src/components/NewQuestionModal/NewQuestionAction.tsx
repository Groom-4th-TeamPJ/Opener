import type { NewQuestionActionProps } from '@/types/exam-variant'
import Button from '@/components/common/Button'

export default function NewQuestionAction({
  isSubmitted,
  selected,
  onSubmit,
  onBack,
}: NewQuestionActionProps) {
  return (
    <>
      {!isSubmitted ? (
        <Button className="flex-1 h-[48]" disabled={selected === null} onClick={onSubmit}>
          제출하기
        </Button>
      ) : (
        <Button className="flex-1 h-[48]" onClick={onBack}>
          원래 문제로 돌아가기
        </Button>
      )}
    </>
  )
}
