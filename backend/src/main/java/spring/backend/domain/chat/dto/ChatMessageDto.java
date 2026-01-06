package spring.backend.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Redis에 저장될 임시 채팅 메시지 DTO
 * <p>
 * Redis Key: chat:session:{sessionId} Redis Value: List<ChatMessageDto> (JSON array) TTL: 24 hours
 * <p>
 * 사용 시나리오: 1. WebSocket으로 실시간 채팅 → Redis에 저장 2. 사용자가 "저장" 버튼 클릭 → PostgreSQL로 마이그레이션
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

  @JsonProperty("sessionId")
  private String sessionId;

  // 메시지 역할: USER, LLM
  @JsonProperty("role")
  private String role;

  // 메세지 내용
  @JsonProperty("content")
  private String content;

  // 메세지 생성 시간
  @JsonProperty("timestamp")
  private LocalDateTime timestamp;

  // 메세지 순서
  @JsonProperty("order")
  private Integer order;
}
