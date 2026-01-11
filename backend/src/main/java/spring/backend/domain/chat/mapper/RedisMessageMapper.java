package spring.backend.domain.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.MessageDto;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.model.entity.ChatMessageContent;

@Component
public class RedisMessageMapper {

    public MessageDto toDtoUser(ChatSendRequest req) {

        return MessageDto.builder()
                .chatRole(ChatRole.USER)
                .message(req.message())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ChatMessageContent toEntity(MessageDto dto) {
        return ChatMessageContent.builder()
                .role(dto.chatRole())
                .content(dto.message())
                .timestamp(dto.timestamp())
                .build();
    }
    
    public List<ChatMessageContent> toEntityList(List<MessageDto> dtoList) {
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
