package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
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
    private final ChatMessageProducer chatMessageProducer;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    // 세션 TTL 과 같은 값 -> 이 시간을 넘긴 이벤트의 빈 버퍼는 정상 만료로 본다
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    // @RabbitListener -> 큐 메시지를 자동 수신, 폴링 코드 없이 이벤트 도착 시 호출
    // @Transactional 은 계속 뺀다 -> LLM 요약 호출은 요약 소비자로 옮겨 갔지만 이 메서드에는 여전히 Redis 읽기·삭제와
    // 큐 발행이 있고, 메서드 전체를 감싸면 그 대기 동안 HikariCP(max 20) 커넥션을 붙잡는다
    // DB 쓰기만 TransactionTemplate 으로 감싸 경계를 최소화하고, 요약 이벤트는 커밋 뒤에 낸다
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_SAVE_QUEUE)
    public void handleSaveMessageEvent(ChatMessageSaveEvent event) {
        // 필수 필드가 빠진 이벤트는 몇 번을 다시 읽어도 같은 결과 -> 재시도 없이 DLQ 로 보낸다
        if (event == null || event.sessionId() == null || event.userId() == null || event.questionResultId() == null) {
            log.error("[RabbitMQ] 저장 이벤트 필드 누락 - event: {}", event);
            meterRegistry.counter("chat.persist.dropped", "reason", "invalid").increment();
            throw new AmqpRejectAndDontRequeueException("저장 이벤트 필드 누락 - 재시도 불가");
        }

        Long sessionId = event.sessionId();
        Long questionResultId = event.questionResultId();

        log.info("[RabbitMQ] 채팅 메시지 저장 이벤트 수신 - sessionId: {}, questionResultId: {}",
                sessionId, questionResultId);

        try {
            // 소유권 검증은 Redis 읽기보다 앞 -> 요청 본문의 questionResultId 는 사용자가 정한 값이라
            // 남의 문항 결과에 내 대화를 붙이거나 남의 결과를 덮어쓰는 경로가 된다
            // 재시도해도 같은 결과 -> 아래 catch 가 재시도 없이 DLQ 로 보낸다. ACK 로 버리면 원인이 남지 않는다
            if (!questionResultRepository.existsByIdAndExamResultUserId(questionResultId, event.userId())) {
                log.warn("[RabbitMQ] 문항 결과 소유자 불일치 - questionResultId: {}, userId: {}",
                        questionResultId, event.userId());
                meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                throw new BusinessException(ErrorCode.AUTHORIZATION_FAILED);
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
                // 코드를 값으로 비교하므로 ErrorCode 를 나눌 때 여기를 같이 고쳐야 분기가 조용히 빠지지 않는다
                // 소유자 불일치는 문항 결과 소유자 불일치와 같은 영구 실패 -> 던져 두면 아래 catch 가 원인을 단 채 DLQ 로 보낸다
                if (e.getErrorCode() == ErrorCode.SESSION_ACCESS_DENIED) {
                    log.warn("[RabbitMQ] 세션 소유자 불일치 - sessionId: {}, userId: {}", sessionId, event.userId());
                    meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                    throw e;
                }
                // 세션 부재는 TTL 만료라 정상 ACK 대상이다
                if (e.getErrorCode() == ErrorCode.SESSION_NOT_FOUND) {
                    log.warn("[RabbitMQ] 세션 부재 - sessionId: {}, userId: {}. 메시지 무시", sessionId, event.userId());
                    meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                    return;
                }
                throw e;
            }

            // 끝나지 않은 턴은 영구 기록에 넣지 않는다 -> 넣으면 복습 화면이 끊긴 문장을 완결된 풀이처럼 보여 준다
            // 규칙(partial 응답 · 답 없는 사용자 메시지 제외)은 요약 읽기와 같은 RedisMessageDto.completeTurnsOf 하나다
            // 제외한 개수는 메트릭으로 남긴다
            List<RedisMessageDto> completeMessages = RedisMessageDto.completeTurnsOf(redisMessages);
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

            // 요약 없이 저장한다 -> 요약은 저장된 행 id 를 받는 요약 소비자가 따로 채운다
            // 요약 호출을 여기에 두면 건당 4.5 초만큼 저장이 늦어져 큐가 쌓인다
            Long chatMessageId = transactionTemplate.execute(status -> {
                QuestionResult questionResult = questionResultRepository.findById(questionResultId)
                        .orElseThrow(() -> {
                            log.error("[RabbitMQ] QuestionResult 조회 실패 - questionResultId: {}", questionResultId);
                            return new BusinessException(ErrorCode.RESULT_NOT_FOUND);
                        });

                ChatMessage chatMessage = ChatMessage.createFromSession(
                        questionResult, messageContents, null);
                return chatMessageRepository.save(chatMessage).getId();
            });

            log.info("[RabbitMQ] 채팅 메시지 저장 완료 - sessionId: {}, 메시지 수: {}",
                    sessionId, messageContents.size());

            // DB 영속화 성공 후 Redis 정리 -> 버퍼는 임시 저장소이므로 영구 저장 완료 시점에 비움
            chatRedisService.deleteMessage(sessionId);

            // 커밋과 버퍼 삭제 뒤에 발행 -> 요약 소비자가 아직 없는 행을 찾는 일이 없다
            publishSummaryEvent(chatMessageId);

        } catch (BusinessException e) {
            log.error("[RabbitMQ] 비즈니스 예외 발생 - sessionId: {}, errorCode: {}, message: {}",
                    sessionId, e.getErrorCode(), e.getMessage());
            // 영구 실패(문항 결과 부재 · 문항 결과 소유자 불일치 · 세션 소유자 불일치)는 재시도해도 같은 결과
            // -> 재시도 횟수만큼 같은 실패를 반복하지 않고 원인을 단 채 바로 DLQ 로 보낸다
            // 재시도 제외는 RabbitListenerRetryPolicyCustomizer 빈이 맡고, RabbitMQConfig 의 리스너 팩토리가
            // Boot 의 SimpleRabbitListenerContainerFactoryConfigurer 를 거치므로 그 빈이 이 컨슈머의 재시도 정책에 닿는다
            if (e.getErrorCode() == ErrorCode.RESULT_NOT_FOUND
                    || e.getErrorCode() == ErrorCode.AUTHORIZATION_FAILED
                    || e.getErrorCode() == ErrorCode.SESSION_ACCESS_DENIED) {
                throw new AmqpRejectAndDontRequeueException("재시도 불가 - " + e.getErrorCode().getCode(), e);
            }
            // 그 밖의 예외는 그대로 던짐 -> Redis 읽기 실패 같은 일시 장애는 재시도가 걸리고, 소진되면 DLQ 로 간다
            throw e;

        } catch (Exception e) {
            log.error("[RabbitMQ] 예기치 않은 예외 발생 - sessionId: {}", sessionId, e);
            // 알 수 없는 예외도 BusinessException 으로 감싸 일관된 처리 경로 유지
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    // 발행 실패는 저장 실패가 아니다 -> 행은 이미 커밋됐고 버퍼도 지워서 저장 이벤트를 재시도해도 CHAT_BUFFER_LOST 로 DLQ 에만 간다
    // 던지는 대신 메트릭과 로그로 남기고, 요약이 빈 행은 API 가 summary 를 null 로 내려 읽는 쪽이 견딘다
    private void publishSummaryEvent(Long chatMessageId) {
        try {
            chatMessageProducer.publishSummaryEvent(chatMessageId);
        } catch (RuntimeException e) {
            log.error("[RabbitMQ] 요약 이벤트 발행 실패 - 요약 없이 저장됨 - chatMessageId: {}", chatMessageId, e);
            meterRegistry.counter("chat.summary.publish_failed").increment();
        }
    }
}
