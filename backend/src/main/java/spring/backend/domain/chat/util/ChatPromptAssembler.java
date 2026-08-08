package spring.backend.domain.chat.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

// 프롬프트 조립 규칙을 순수 함수로 분리 -> 복제 지연·저장 실패와 무관하게 질문 포함을 단위 테스트로 고정
public final class ChatPromptAssembler {

    private ChatPromptAssembler() {
    }

    // 히스토리는 replica 에서 읽어 stale 일 수 있다
    // 방금 쓴 질문이 빠지면 LLM 이 직전 턴에 다시 답하므로 호출자가 받은 원문을 명시적으로 보장한다
    public static List<Message> withCurrentQuestion(List<Message> history, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return history;
        }

        String lastUserText = history.stream()
                .filter(m -> m instanceof UserMessage)
                .reduce((first, second) -> second)
                .map(Message::getText)
                .orElse(null);

        if (userMessage.equals(lastUserText)) {
            return history;
        }

        List<Message> merged = new ArrayList<>(history);
        merged.add(new UserMessage(userMessage));
        return merged;
    }
}
