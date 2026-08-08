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
        // 전이 선언은 ChatStateTransitions.ALL 하나에서만 온다
        // 여기에 직접 적으면 테스트가 보는 값과 갈라져, 테이블을 고쳐도 설정은 그대로인 사고가 난다
        // 명시 안 한 (상태, 이벤트) 조합은 자동 거부되어 잘못된 흐름이 차단된다
        StateMachineTransitionConfigurer<ChatSessionState, ChatSessionEvent> config = transitions;
        for (ChatStateTransition transition : ChatStateTransitions.ALL) {
            config = config.withExternal()
                    .source(transition.source())
                    .target(transition.target())
                    .event(transition.event())
                    .and();
        }
    }
}
