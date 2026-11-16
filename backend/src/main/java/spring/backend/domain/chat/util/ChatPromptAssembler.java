package spring.backend.domain.chat.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

// 프롬프트 조립 규칙을 순수 함수로 분리 -> 복제 지연·저장 실패와 무관하게 질문 포함을 단위 테스트로 고정
public final class ChatPromptAssembler {

    private ChatPromptAssembler() {
    }

    // 히스토리는 방금 저장한 질문 직전까지만 읽으므로 현재 질문은 들어 있지 않다 -> 호출자가 받은 원문을 한 번 붙인다
    // 저장이 실패한 턴도 같은 경로라 질문이 빠져 LLM 이 직전 턴에 다시 답하는 일이 없다
    // 마지막 메시지가 같은 글자의 사용자 메시지일 때만 붙이지 않는다 -> 저장은 됐는데 응답만 잃은 턴이 읽혔을 때 질문이 두 번 실리지 않게 한다
    // 마지막 "사용자 메시지" 와 비교하면 "계속" 처럼 반복되는 질문이 빠져 모델이 직전 답에 다시 답한다
    public static List<Message> withCurrentQuestion(List<Message> history, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return history;
        }

        if (!history.isEmpty()
                && history.get(history.size() - 1) instanceof UserMessage last
                && userMessage.equals(last.getText())) {
            return history;
        }

        List<Message> merged = new ArrayList<>(history);
        merged.add(new UserMessage(userMessage));
        return merged;
    }
}
