package spring.backend.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// RAG 비활성 환경용 폴백 빈
// 이 빈이 없으면 app.rag.enabled=false 에서 ChatServiceImpl 이 RagService 를 주입받지 못해
// 채팅 도메인 전체가 기동 실패한다 (오프너 분석만 못 쓰는 것이 아니다)
class DisabledRagServiceTest {

    private final DisabledRagService service = new DisabledRagService();

    @Test
    @DisplayName("RAG 비활성 상태에서 오프너 분석을 요청하면 RAG_DISABLED 로 실패한다")
    void generateSimilarProblemStream_disabled_error() {
        StepVerifier.create(service.generateSimilarProblemStream("문제 컨텍스트"))
                .expectErrorSatisfies(t -> {
                    BusinessException ex = assertInstanceOfBusinessException(t);
                    assertEquals(ErrorCode.RAG_DISABLED, ex.getErrorCode());
                })
                .verify();
    }

    // 빈 스트림이 아니라 error 여야 한다 -> 조용히 완료되면 호출부의 doOnComplete 가
    // STREAM_START 없이 실행돼 세션이 고착되던 그 경로를 다시 만든다
    @Test
    @DisplayName("조용히 완료되지 않고 반드시 에러로 끝난다")
    void generateSimilarProblemStream_doesNotCompleteEmpty() {
        StepVerifier.create(service.generateSimilarProblemStream("문제 컨텍스트"))
                .expectError(BusinessException.class)
                .verify();
    }

    private BusinessException assertInstanceOfBusinessException(Throwable t) {
        if (t instanceof BusinessException be) {
            return be;
        }
        throw new AssertionError("BusinessException 이 아님: " + t.getClass().getName());
    }
}
