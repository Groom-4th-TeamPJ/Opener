import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'
import { useExamResult } from '@/hooks/exam/queries/use-exam-result'
import { useSaveChatMessage } from '@/hooks/exam/queries/use-save-message'
import { useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-key'
import type { SubmitAnswerResponse } from '@/types/exam'
import { useDisconnectChat } from '@/hooks/exam/queries/use-disconnect-chat'
import { toast } from 'sonner'
import { gaEvent } from '@/utils/ga'

interface NavigationButtonProps {
  isLastQuestion: boolean
  onNext: () => void
}

export default function NavigationButton({ isLastQuestion, onNext }: NavigationButtonProps) {
  const { currentIndex, getQuestionState } = useExamStore()
  const examData = useCurrentExam()
  const { refetch, isFetching } = useExamResult()
  const { mutateAsync: saveChatMessage } = useSaveChatMessage()
  const queryClient = useQueryClient()
  const { mutate: disconnectChat } = useDisconnectChat()

  const questionId = examData?.questions[currentIndex]?.questionId ?? 0
  const streamingMessage = useExamStore((state) => state.streamingMessages[questionId] ?? '')
  const isStreaming = !!streamingMessage

  if (!examData) return null

  const { questions } = examData
  const question = questions[currentIndex]
  const { isSubmitted, isAnalysisActive } = getQuestionState(question.questionId)
  const submitResult = queryClient.getQueryData<SubmitAnswerResponse>(
    QUERY_KEYS.EXAM.SUBMIT_RESULT(question.questionId)
  )

  const handleClick = async () => {
    if (submitResult?.questionResultId && isAnalysisActive) {
      await saveChatMessage({
        sessionId: examData.examResultId,
        questionResultId: submitResult.questionResultId,
      })
      gaEvent('scrapbook_event', {
        action_type: 'save',
        questionId: question.questionId,
      })
    }

    if (isLastQuestion) {
      disconnectChat(examData.examResultId)

      const { isSuccess } = await refetch()
      if (isSuccess) {
        onNext()
      } else {
        toast.error('학습 결과를 불러오는데 실패했습니다. 다시 시도해주세요.', { duration: 3000 })
      }
      return
    }
    onNext()
  }

  return (
    <Button
      onClick={isSubmitted && !isStreaming ? handleClick : undefined}
      disabled={!isSubmitted || isStreaming}
      isLoading={isFetching}
      variant="default"
      size="lg"
      className={cn(
        'bg-white text-primary-600 hover:text-primary-500 hover:bg-neutral-50 active:text-primary-700 active:bg-neutral-100 whitespace-nowrap',
        !isSubmitted && 'cursor-not-allowed'
      )}
    >
      {isLastQuestion ? '학습종료' : '다음'}
    </Button>
  )
}
