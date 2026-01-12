package spring.backend.domain.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.model.entity.ChatMessageContent;

@Component
public class RedisMessageMapper {

    // 사용자 메세지
    public RedisMessageDto toDtoUser(ChatSendRequest req) {

        return RedisMessageDto.builder()
                .chatRole(ChatRole.USER)
                .message(req.message())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // LLM 메세지
    public RedisMessageDto toDtoLlm(String llmResponse) {
        return RedisMessageDto.builder()
                .chatRole(ChatRole.LLM)
                .message(llmResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ChatMessageContent toEntity(RedisMessageDto dto) {
        return ChatMessageContent.builder()
                .role(dto.chatRole())
                .content(dto.message())
                .timestamp(dto.timestamp())
                .build();
    }

    public List<ChatMessageContent> toEntityList(List<RedisMessageDto> dtoList) {
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
