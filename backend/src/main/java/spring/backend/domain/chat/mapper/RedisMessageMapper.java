package spring.backend.domain.chat.mapper;

import java.time.LocalDateTime;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.MessageInputDto;
import spring.backend.domain.chat.dto.request.ChatSendRequest;

public class RedisMessageMapper {

    public MessageInputDto toDtoUser(ChatSendRequest req) {

        return MessageInputDto.builder()
                .chatRole(ChatRole.USER)
                .message(req.message())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
