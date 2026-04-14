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

//  채팅 세션 State Machine 설정
@Slf4j
@Configuration
@EnableStateMachineFactory
public class ChatStateMachineConfig
        extends EnumStateMachineConfigurerAdapter<ChatSessionState, ChatSessionEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<ChatSessionState, ChatSessionEvent> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
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
                .initial(ChatSessionState.IDLE)
                .states(EnumSet.allOf(ChatSessionState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ChatSessionState, ChatSessionEvent> transitions)
            throws Exception {
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
