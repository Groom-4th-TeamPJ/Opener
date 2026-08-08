package spring.backend.domain.chat.util;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// 복제 지연으로 히스토리에 방금 쓴 질문이 없어도 LLM 이 질문 자체는 반드시 보게 한다
class ChatPromptAssemblerTest {

    @Test
    @DisplayName("히스토리에 현재 질문이 없으면 맨 뒤에 붙인다")
    void withCurrentQuestion_히스토리에질문없음_뒤에추가된다() {
        List<Message> history = List.of(
                new UserMessage("이전 질문"),
                new AssistantMessage("이전 답변"));

        List<Message> result = ChatPromptAssembler.withCurrentQuestion(history, "현재 질문");

        assertEquals(3, result.size());
        assertEquals("현재 질문", result.get(2).getText());
    }

    @Test
    @DisplayName("히스토리 마지막 사용자 메시지가 현재 질문과 같으면 중복 추가하지 않는다")
    void withCurrentQuestion_이미포함됨_그대로반환한다() {
        List<Message> history = List.of(
                new AssistantMessage("이전 답변"),
                new UserMessage("현재 질문"));

        List<Message> result = ChatPromptAssembler.withCurrentQuestion(history, "현재 질문");

        assertSame(history, result);
    }

    @Test
    @DisplayName("히스토리가 비어 있으면 현재 질문만 담긴 목록을 만든다")
    void withCurrentQuestion_히스토리비어있음_질문만담긴다() {
        List<Message> result = ChatPromptAssembler.withCurrentQuestion(List.of(), "현재 질문");

        assertEquals(1, result.size());
        assertEquals("현재 질문", result.get(0).getText());
    }

    @Test
    @DisplayName("현재 질문이 공백이면 히스토리를 그대로 반환한다")
    void withCurrentQuestion_질문공백_그대로반환한다() {
        List<Message> history = List.of(new UserMessage("이전 질문"));

        assertSame(history, ChatPromptAssembler.withCurrentQuestion(history, "   "));
    }
}
