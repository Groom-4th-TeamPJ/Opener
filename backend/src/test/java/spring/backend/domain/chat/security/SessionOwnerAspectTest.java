package spring.backend.domain.chat.security;

import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 진입점이 늘어나도 검증이 새지 않는지를 어드바이스 단위로 고정한다
@ExtendWith(MockitoExtension.class)
class SessionOwnerAspectTest {

    private static final Long SESSION_ID = 7L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private ChatRedisService chatRedisService;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private org.aspectj.lang.Signature signature;
    @InjectMocks private SessionOwnerAspect aspect;

    @Test
    @DisplayName("Long sessionId 와 UUID 인자를 받는 메서드에서 소유자 검증이 강제된다")
    void enforce_스칼라인자_검증후진행한다() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{SESSION_ID, USER_ID});
        when(joinPoint.proceed()).thenReturn(null);

        aspect.enforce(joinPoint);

        verify(chatRedisService).validateSessionOwner(SESSION_ID, USER_ID);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("SessionScoped 요청 DTO 에서도 sessionId 를 뽑아 검증한다")
    void enforce_요청DTO인자_검증후진행한다() throws Throwable {
        ChatSendRequest req = new ChatSendRequest(SESSION_ID, 10L, "질문");
        when(joinPoint.getArgs()).thenReturn(new Object[]{req, USER_ID});
        when(joinPoint.proceed()).thenReturn(null);

        aspect.enforce(joinPoint);

        verify(chatRedisService).validateSessionOwner(SESSION_ID, USER_ID);
    }

    @Test
    @DisplayName("검증이 실패하면 대상 메서드를 실행하지 않는다")
    void enforce_소유자불일치_대상메서드를실행하지않는다() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{SESSION_ID, USER_ID});
        doThrow(new BusinessException(ErrorCode.SESSION_ACCESS_DENIED))
                .when(chatRedisService).validateSessionOwner(SESSION_ID, USER_ID);

        BusinessException thrown = assertThrows(BusinessException.class, () -> aspect.enforce(joinPoint));

        assertEquals(ErrorCode.SESSION_ACCESS_DENIED, thrown.getErrorCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("sessionId 를 찾을 수 없으면 조용히 통과시키지 않고 거부한다")
    void enforce_세션ID없음_예외를던진다() {
        // 규약을 못 맞춘 메서드를 통과시키면 애너테이션이 방어가 아니라 장식이 된다
        when(joinPoint.getArgs()).thenReturn(new Object[]{"문자열", USER_ID});
        // 거부 로그가 시그니처를 찍으므로 목을 채운다 (실제 조인포인트는 항상 시그니처를 갖는다)
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ChatServiceImpl.someMethod(..)");

        BusinessException thrown = assertThrows(BusinessException.class, () -> aspect.enforce(joinPoint));

        assertEquals(ErrorCode.INVALID_SESSION, thrown.getErrorCode());
    }
}
