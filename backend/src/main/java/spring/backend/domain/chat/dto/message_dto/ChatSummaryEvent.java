package spring.backend.domain.chat.dto.message_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

// 저장된 행 id 만 담는다 -> 요약 소비자가 행의 messages 를 읽으므로 큐에 대화 본문을 싣지 않는다
public record ChatSummaryEvent(
        @JsonProperty("chatMessageId")
        Long chatMessageId
) implements Serializable {
}
