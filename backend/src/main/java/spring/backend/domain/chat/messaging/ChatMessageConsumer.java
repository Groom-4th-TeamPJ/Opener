package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.model.entity.ChatMessageContent;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    // 모든 협력 객체를 final 로 주입 -> 불변 의존성 + 외부 재할당 차단으로 안전하게 사용
    private final ChatRedisService chatRedisService;
    private final RedisMessageMapper redisMessageMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final QuestionResultRepository questionResultRepository;
    private final LlmService llmService;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    // 세션 TTL 과 같은 값 -> 이 시간을 넘긴 이벤트의 빈 버퍼는 정상 만료로 본다
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    // @RabbitListener -> 큐 메시지를 자동 수신, 폴링 코드 없이 이벤트 도착 시 호출
    // @Transactional 제거 -> LLM 요약은 십수 초가 걸리는 외부 호출이라 커넥션을 붙잡은 채 기다리면
    // HikariCP(max 20)가 고갈되고, 그것이 곧 이 도메인이 막겠다고 한 연쇄 지연이다
    // DB 쓰기만 TransactionTemplate 으로 감싸 경계를 최소화한다
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_SAVE_QUEUE)
    public void handleSaveMessageEvent(ChatMessageSaveEvent event) {
        Long sessionId = event.sessionId();
        Long questionResultId = event.questionResultId();

        log.info("[RabbitMQ] 채팅 메시지 저장 이벤트 수신 - sessionId: {}, questionResultId: {}",
                sessionId, questionResultId);

        try {
            // 소유권 검증은 Redis 읽기와 LLM 요약보다 앞 -> 요청 본문의 questionResultId 는 사용자가 정한 값이라
            // 남의 문항 결과에 내 대화를 붙이거나 남의 결과를 덮어쓰는 경로가 된다
            // 재시도해도 같은 결과라 DLQ 가 아니라 ACK 한다 (아래 세션 소유자 불일치 처리와 같다)
            if (!questionResultRepository.existsByIdAndExamResultUserId(questionResultId, event.userId())) {
                log.warn("[RabbitMQ] 문항 결과 소유자 불일치 - questionResultId: {}, userId: {}. 메시지 무시",
                        questionResultId, event.userId());
                meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                return;
            }

            // Redis에서 세션 메시지 조회
            List<RedisMessageDto> redisMessages = chatRedisService.getSessionMessages(sessionId);

            // 버퍼가 비었을 때 정상 만료와 비정상 소실을 가른다
            // publishedAt 이 없으면(구 메시지) 판정 근거가 없으므로 안전한 쪽인 정상 만료로 본다
            if (redisMessages == null || redisMessages.isEmpty()) {
                Instant publishedAt = event.publishedAt();
                boolean expired = publishedAt == null
                        || Duration.between(publishedAt, Instant.now()).compareTo(SESSION_TTL) >= 0;

                if (expired) {
                    // TTL 을 넘겼으면 사라진 것이 정상 -> ACK 하되 사실은 남긴다
                    log.warn("[RabbitMQ] 세션 TTL 만료로 버퍼 비어 있음 - sessionId: {}", sessionId);
                    meterRegistry.counter("chat.persist.dropped", "reason", "expired").increment();
                    return;
                }

                // TTL 안인데 비어 있다 = 복제 유실·LRU 축출 -> 조용히 ACK 하면 유실을 유실로 알 수 없다
                log.error("[RabbitMQ] TTL 이내인데 버퍼가 비어 있음 - sessionId: {}, publishedAt: {}",
                        sessionId, publishedAt);
                meterRegistry.counter("chat.persist.dropped", "reason", "lost").increment();
                throw new BusinessException(ErrorCode.CHAT_BUFFER_LOST);
            }

            // 권한 검증 (Redis에 데이터가 있는 경우에만 검증)
            // 어드바이스(@RequireSessionOwner) 대상에서 제외 -> 이벤트는 userId 를 인자가 아니라 페이로드로 갖고
            // 검증 실패 시 처리도 HTTP 거부가 아니라 ACK/DLQ 결정이라 진입 규약이 다르다
            try {
                chatRedisService.validateSessionOwner(sessionId, event.userId());
            } catch (BusinessException e) {
                // 세션 부재·소유자 불일치는 정상 ACK 대상이다
                // 코드를 값으로 비교하므로 ErrorCode 를 나눌 때 여기를 같이 고쳐야 분기가 조용히 빠지지 않는다
                if (e.getErrorCode() == ErrorCode.SESSION_NOT_FOUND
                        || e.getErrorCode() == ErrorCode.SESSION_ACCESS_DENIED) {
                    log.warn("[RabbitMQ] 세션 권한 검증 실패 - sessionId: {}, userId: {}. 메시지 무시",
                            sessionId, event.userId());
                    meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                    // 권한 없는 경우도 정상 처리로 간주
                    return;
                }
                throw e;
            }

            // 끝나지 않은 턴은 영구 기록에 넣지 않는다 -> 넣으면 복습 화면이 끊긴 문장을 완결된 풀이처럼 보여 준다
            // (1) 스트림이 끊겨 받은 만큼만 저장된 응답(partial 표식)
            // (2) 끝에 답 없이 남은 사용자 메시지 (첫 청크 전에 끊겼거나 아직 스트리밍 중인 턴)
            // 완결된 턴만 남기고 제외한 개수는 메트릭으로 남긴다
            List<RedisMessageDto> completeMessages = new ArrayList<>(redisMessages.stream()
                    .filter(message -> !message.partial())
                    .toList());
            while (!completeMessages.isEmpty()
                    && completeMessages.get(completeMessages.size() - 1).chatRole() == ChatRole.USER) {
                completeMessages.remove(completeMessages.size() - 1);
            }
            int truncated = redisMessages.size() - completeMessages.size();
            if (truncated > 0) {
                log.warn("[RabbitMQ] 끝나지 않은 턴의 메시지 {}건 제외 - sessionId: {}", truncated, sessionId);
                meterRegistry.counter("chat.persist.dropped", "reason", "truncated").increment(truncated);
            }
            if (completeMessages.isEmpty()) {
                return;
            }

            // MessageDto 리스트를 ChatMessageContent 리스트로 변환 -> 현재 구성은 동일하지만 추후 확장성 고려
            List<ChatMessageContent> messageContents = redisMessageMapper.toEntityList(completeMessages);

            // 트랜잭션 밖에서 먼저 요약 -> 외부 호출 지연이 DB 커넥션 점유 시간에 더해지지 않게 한다
            String summary = llmService.summaryChat(sessionId.toString());

            transactionTemplate.execute(status -> {
                QuestionResult questionResult = questionResultRepository.findById(questionResultId)
                        .orElseThrow(() -> {
                            log.error("[RabbitMQ] QuestionResult 조회 실패 - questionResultId: {}", questionResultId);
                            return new BusinessException(ErrorCode.RESULT_NOT_FOUND);
                        });

                ChatMessage chatMessage = ChatMessage.createFromSession(
                        questionResult, messageContents, summary);
                chatMessageRepository.save(chatMessage);
                return null;
            });

            log.info("[RabbitMQ] 채팅 메시지 저장 완료 - sessionId: {}, 메시지 수: {}",
                    sessionId, messageContents.size());

            // DB 영속화 성공 후 Redis 정리 -> 버퍼는 임시 저장소이므로 영구 저장 완료 시점에 비움
            chatRedisService.deleteMessage(sessionId);

        } catch (BusinessException e) {
            log.error("[RabbitMQ] 비즈니스 예외 발생 - sessionId: {}, errorCode: {}, message: {}",
                    sessionId, e.getErrorCode(), e.getMessage());
            // 문항 결과 부재는 재시도해도 같은 결과 -> 재시도 횟수만큼 summaryChat 을 다시 부르지 않고 바로 DLQ 로 보낸다
            if (e.getErrorCode() == ErrorCode.RESULT_NOT_FOUND) {
                throw new AmqpRejectAndDontRequeueException("문항 결과 없음 - 재시도 불가", e);
            }
            // 그 밖의 예외는 그대로 던짐 -> Redis 읽기 실패 같은 일시 장애는 재시도가 걸리고, 소진되면 DLQ 로 간다
            throw e;

        } catch (Exception e) {
            log.error("[RabbitMQ] 예기치 않은 예외 발생 - sessionId: {}", sessionId, e);
            // 알 수 없는 예외도 BusinessException 으로 감싸 일관된 처리 경로 유지
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }
}
