package spring.backend.domain.chat.statemachine;

import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

// 채팅 세션 State Machine 설정
@Slf4j
@Configuration
// @EnableStateMachineFactory -> 단일 머신이 아닌 Factory 활성화, 세션마다 독립 인스턴스를 찍어내기 위함
@EnableStateMachineFactory
public class ChatStateMachineConfig
        extends EnumStateMachineConfigurerAdapter<ChatSessionState, ChatSessionEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<ChatSessionState, ChatSessionEvent> config)
            throws Exception {
        config
                .withConfiguration()
                // autoStartup -> 머신 생성 즉시 초기 상태로 진입, 호출부에서 start 누락 사고 방지
                .autoStartup(true)
                // 리스너로 전이 로그만 남김 -> 운영 중 상태 흐름 추적용, 비즈니스 로직은 넣지 않음
                .listener(new StateMachineListenerAdapter<>() {
                    @Override
                    public void stateChanged(State<ChatSessionState, ChatSessionEvent> from,
                                             State<ChatSessionState, ChatSessionEvent> to) {
                        if (from != null) {
                            log.debug("[StateMachine] 상태 전이: {} → {}", from.getId(), to.getId());
                        }
                    }
                });
    }

    @Override
    public void configure(StateMachineStateConfigurer<ChatSessionState, ChatSessionEvent> states)
            throws Exception {
        states
                .withStates()
                // 연결 전이 기본값이므로 IDLE 을 시작 상태로 고정
                .initial(ChatSessionState.IDLE)
                // enum 전체를 상태로 등록 -> 상태 추가 시 enum 만 늘리면 자동 반영
                .states(EnumSet.allOf(ChatSessionState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ChatSessionState, ChatSessionEvent> transitions)
            throws Exception {
        // 허용된 (상태, 이벤트) 쌍만 전이로 선언 -> 명시 안 한 조합은 자동 거부되어 잘못된 흐름 차단
        transitions
                // IDLE → CONNECTED (연결 성공)
                .withExternal()
                .source(ChatSessionState.IDLE)
                .target(ChatSessionState.CONNECTED)
                .event(ChatSessionEvent.CONNECT_SUCCESS)
                .and()

                // IDLE → CONNECTION_ERROR (연결 실패 — 관찰 가능한 실패 상태 진입)
                .withExternal()
                .source(ChatSessionState.IDLE)
                .target(ChatSessionState.CONNECTION_ERROR)
                .event(ChatSessionEvent.CONNECT_FAIL)
                .action(context ->
                        log.warn("[StateMachine] CONNECT_FAIL - 연결 실패 상태로 전이"))
                .and()

                // CONNECTED → PROCESSING (메시지 전송)
                .withExternal()
                .source(ChatSessionState.CONNECTED)
                .target(ChatSessionState.PROCESSING)
                .event(ChatSessionEvent.SEND_MESSAGE)
                .and()

                // PROCESSING → STREAMING (첫 청크 수신)
                .withExternal()
                .source(ChatSessionState.PROCESSING)
                .target(ChatSessionState.STREAMING)
                .event(ChatSessionEvent.STREAM_START)
                .and()

                // PROCESSING → PROCESSING_ERROR (스트리밍 시작 전 LLM/RAG 호출 오류)
                .withExternal()
                .source(ChatSessionState.PROCESSING)
                .target(ChatSessionState.PROCESSING_ERROR)
                .event(ChatSessionEvent.STREAM_ERROR)
                .and()

                // STREAMING → COMPLETED (스트리밍 완료)
                .withExternal()
                .source(ChatSessionState.STREAMING)
                .target(ChatSessionState.COMPLETED)
                .event(ChatSessionEvent.STREAM_COMPLETE)
                .and()

                // STREAMING → STREAM_ERROR (청크 수신 중 오류)
                .withExternal()
                .source(ChatSessionState.STREAMING)
                .target(ChatSessionState.STREAM_ERROR)
                .event(ChatSessionEvent.STREAM_ERROR)
                .and()

                // COMPLETED → PROCESSING (연속 대화 — 새 메시지 전송)
                .withExternal()
                .source(ChatSessionState.COMPLETED)
                .target(ChatSessionState.PROCESSING)
                .event(ChatSessionEvent.SEND_MESSAGE)
                .and()

                // CONNECTED → IDLE (세션 종료)
                .withExternal()
                .source(ChatSessionState.CONNECTED)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // COMPLETED → IDLE (세션 종료)
                .withExternal()
                .source(ChatSessionState.COMPLETED)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // PROCESSING → IDLE (처리 중 세션 종료)
                .withExternal()
                .source(ChatSessionState.PROCESSING)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // STREAMING → IDLE (스트리밍 중 세션 종료)
                .withExternal()
                .source(ChatSessionState.STREAMING)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // STREAM_ERROR → IDLE (스트리밍 오류 후 종료)
                .withExternal()
                .source(ChatSessionState.STREAM_ERROR)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // PROCESSING_ERROR → IDLE (처리 오류 후 종료)
                .withExternal()
                .source(ChatSessionState.PROCESSING_ERROR)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE)
                .and()

                // CONNECTION_ERROR → IDLE (연결 오류 후 종료)
                .withExternal()
                .source(ChatSessionState.CONNECTION_ERROR)
                .target(ChatSessionState.IDLE)
                .event(ChatSessionEvent.CLOSE);
    }
}
