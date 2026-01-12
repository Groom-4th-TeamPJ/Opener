package spring.backend.domain.chat.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.backend.domain.chat.dto.enums.ChatRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageContent {

    // 메시지 역할: USER, LLM
    @JsonProperty("role")
    private ChatRole role;

    // 메세지 내용
    @JsonProperty("content")
    private String content;

    // 메세지 생성 시간
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
