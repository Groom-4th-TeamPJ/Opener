package spring.backend.domain.chat.service.impl;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// Claude 스트림 -> 텍스트 조각 변환
// stop_reason refusal 이나 텍스트가 한 조각도 없는 응답은 정상 완료로 두면 사용자가 빈 답변만 본다 -> LLM 응답 실패 에러로 끝낸다
// BusinessException 으로 던진다 -> llm-* 서킷은 도메인 판정으로 보고 세지 않고, 한 대화의 거절이 서킷을 열지 않는다
final class ClaudeStreams {

    private ClaudeStreams() {}

    // tokenGap -> 첫 조각을 포함해 이 시간 동안 아무 조각도 안 오면 TimeoutException (총 시간 상한이 아니라 읽기 사이 간격)
    // Anthropic SDK 의 읽기 제한은 spring.ai.anthropic.timeout 하나로 전체 호출 상한과 같은 값이라 간격만 따로 걸 수 없다 -> Reactor timeout 을 쓴다
    // TimeoutException 은 BusinessException 이 아니므로 llm-chat 서킷이 전환 전처럼 실패로 센다
    static Flux<String> text(Flux<ChatResponse> responses, Duration tokenGap) {
        return Flux.defer(() -> {
            AtomicBoolean refused = new AtomicBoolean();
            AtomicBoolean emitted = new AtomicBoolean();
            return responses
                    .timeout(tokenGap)
                    .concatMap(response -> {
                        if (isRefusal(response)) {
                            refused.set(true);
                        }
                        String text = textOf(response);
                        if (text.isEmpty()) {
                            return Flux.<String>empty();
                        }
                        emitted.set(true);
                        return Flux.just(text);
                    })
                    .concatWith(Flux.defer(() -> refused.get() || !emitted.get()
                            ? Flux.error(new BusinessException(ErrorCode.LLM_RESPONSE_FAIL))
                            : Flux.empty()));
        });
    }

    private static String textOf(ChatResponse response) {
        StringBuilder text = new StringBuilder();
        for (Generation generation : response.getResults()) {
            String part = generation.getOutput().getText();
            if (part != null) {
                text.append(part);
            }
        }
        return text.toString();
    }

    // SDK 의 stop_reason 문자열 표기가 대소문자·포장이 다를 수 있다 -> refusal 포함 여부만 대소문자 무시로 본다
    private static boolean isRefusal(ChatResponse response) {
        for (Generation generation : response.getResults()) {
            String reason = generation.getMetadata().getFinishReason();
            if (reason != null && reason.toLowerCase().contains("refusal")) {
                return true;
            }
        }
        return false;
    }
}
