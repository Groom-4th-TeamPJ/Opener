package spring.backend.domain.chat.dto.redis_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import spring.backend.domain.chat.dto.enums.ChatRole;

// record -> 불변 + equals/hashCode 자동, Redis 직렬화 단위로 안전
// @Builder -> 필드 많을 때 가독성 있는 생성, 순서 실수 방지
@Builder
public record RedisMessageDto(

        // @JsonProperty -> 자바 필드명(chatRole)과 JSON 키(role)를 분리, 저장 포맷을 안정적으로 고정
        @JsonProperty("role")
        ChatRole chatRole,

        // 메세지 내용
        @JsonProperty("message")
        String message,

        // 메세지 생성 시간
        @JsonProperty("timestamp")
        LocalDateTime timestamp,

        // 스트림이 끊겨 받은 만큼만 저장된 응답 표식 -> 구 메시지에는 키가 없어 false 로 읽힌다
        @JsonProperty("partial")
        boolean partial
) {

    // 끝난 턴만 남긴다 -> 영구 저장과 요약이 같은 규칙으로 같은 대화를 보게 한다
    // (1) 스트림이 끊겨 받은 만큼만 저장된 응답(partial 표식)을 뺀다
    // (2) 바로 뒤에 LLM 답이 없는 사용자 메시지를 뺀다 (끝에 남았거나, 뒤의 답이 (1) 로 빠져 다음 질문과 맞붙은 경우)
    public static List<RedisMessageDto> completeTurnsOf(List<RedisMessageDto> messages) {
        List<RedisMessageDto> answered = messages.stream()
                .filter(message -> !message.partial())
                .toList();
        List<RedisMessageDto> complete = new ArrayList<>();
        for (int i = 0; i < answered.size(); i++) {
            RedisMessageDto message = answered.get(i);
            boolean unanswered = message.chatRole() == ChatRole.USER
                    && (i + 1 >= answered.size() || answered.get(i + 1).chatRole() != ChatRole.LLM);
            if (!unanswered) {
                complete.add(message);
            }
        }
        return complete;
    }
}
