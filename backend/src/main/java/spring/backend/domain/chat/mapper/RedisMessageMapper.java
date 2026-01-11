package spring.backend.domain.chat.mapper;

import java.time.LocalDateTime;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.MessageDto;
import spring.backend.domain.chat.dto.request.ChatSendRequest;

public class RedisMessageMapper {

    public MessageDto toDtoUser(ChatSendRequest req) {

        return MessageDto.builder()
                .chatRole(ChatRole.USER)
                .message(req.message())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
