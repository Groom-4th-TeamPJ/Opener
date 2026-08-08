package spring.backend.domain.chat.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.request.SessionScoped;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// 판정 로직은 ChatRedisService 가, 진입 통제는 이 어드바이스가 맡는다
// 둘을 나눠야 새 엔드포인트가 생겨도 검증 호출을 잊는 경로가 생기지 않는다
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SessionOwnerAspect {

    private final ChatRedisService chatRedisService;

    @Around("@annotation(spring.backend.domain.chat.security.RequireSessionOwner)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        Long sessionId = extractSessionId(args);
        UUID userId = extractUserId(args);

        // 규약을 못 맞춘 메서드를 조용히 통과시키면 애너테이션이 방어가 아니라 장식이 된다
        if (sessionId == null || userId == null) {
            log.error("[SessionOwner] sessionId/userId 추출 실패 - method: {}",
                    joinPoint.getSignature().toShortString());
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }

        chatRedisService.validateSessionOwner(sessionId, userId);

        return joinPoint.proceed();
    }

    private Long extractSessionId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long id) {
                return id;
            }
            if (arg instanceof SessionScoped scoped) {
                return scoped.sessionId();
            }
        }
        return null;
    }

    private UUID extractUserId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID id) {
                return id;
            }
        }
        return null;
    }
}
